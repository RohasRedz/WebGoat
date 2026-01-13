import argparse
import hashlib
import json
import os
import subprocess
from dataclasses import dataclass
from datetime import datetime
from typing import Dict, List, Optional, Tuple
 
from tree_sitter_languages import get_parser, get_language
 
 
# -----------------------------
# Configuration
# -----------------------------
 
SUPPORTED_EXTENSIONS = {
    ".java": "java",
    ".py": "python",
    ".js": "javascript",
    ".ts": "javascript",
    ".jsx": "javascript",
    ".tsx": "javascript",
}
 
EXCLUDED_DIRS = {
    ".git",
    "scripts",        # exclude infra
    "node_modules",
    "build",
    "dist",
    "target",
    ".gradle",
    ".idea",
    "__pycache__",
}
 
# -----------------------------
# Tree-sitter queries
# -----------------------------
 
JAVA_QUERY = r"""
(class_declaration name: (identifier) @class_name) @class_decl
(method_declaration name: (identifier) @method_name) @method_decl
(constructor_declaration name: (identifier) @ctor_name) @ctor_decl
(method_invocation name: (identifier) @call_name) @call
(object_creation_expression type: (type_identifier) @new_type) @newexpr
"""
 
PY_QUERY = r"""
(class_definition name: (identifier) @class_name) @class_decl
(function_definition name: (identifier) @func_name) @func_decl
(call function: (identifier) @call_name) @call
"""
 
JS_QUERY = r"""
(class_declaration name: (identifier) @class_name) @class_decl
(function_declaration name: (identifier) @func_name) @func_decl
(method_definition name: (property_identifier) @method_name) @method_decl
(call_expression function: (identifier) @call_name) @call
(new_expression constructor: (identifier) @new_ctor) @newexpr
"""
 
# -----------------------------
# Helpers
# -----------------------------
 
def git(cmd: List[str]) -> str:
    return subprocess.check_output(["git"] + cmd).decode().strip()
 
def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()
 
def stable_id(prefix: str, *parts: str) -> str:
    h = hashlib.sha1("::".join(parts).encode("utf-8")).hexdigest()
    return f"{prefix}_{h[:12]}"
 
def to_range(node) -> List[int]:
    return [
        node.start_point[0] + 1,
        node.start_point[1] + 1,
        node.end_point[0] + 1,
        node.end_point[1] + 1,
    ]
 
def node_text(src: bytes, node) -> str:
    return src[node.start_byte:node.end_byte].decode("utf-8", errors="replace")
 
def detect_language(path: str) -> Optional[str]:
    for ext, lang in SUPPORTED_EXTENSIONS.items():
        if path.endswith(ext):
            return lang
    return None
 
def should_exclude_dir(path: str) -> bool:
    parts = path.replace("\\", "/").split("/")
    return any(p in EXCLUDED_DIRS for p in parts)
 
def iter_source_files(root=".") -> List[str]:
    out = []
    for dirpath, dirnames, filenames in os.walk(root):
        if should_exclude_dir(dirpath):
            dirnames[:] = []
            continue
        for f in filenames:
            p = os.path.join(dirpath, f)
            if detect_language(p):
                out.append(p)
    return out
 
# -----------------------------
# Data models
# -----------------------------
 
@dataclass
class Symbol:
    id: str
    kind: str
    name: str
    file: str
    range: List[int]
 
@dataclass
class Ref:
    id: str
    kind: str
    name: str
    file: str
    range: List[int]
    candidates: List[str]
 
# -----------------------------
# Extraction
# -----------------------------
 
def extract_file(path: str, lang: str, src: bytes):
    parser = get_parser(lang)
    language = get_language(lang)
 
    tree = parser.parse(src)
    root = tree.root_node
 
    query = language.query(
        JAVA_QUERY if lang == "java"
        else PY_QUERY if lang == "python"
        else JS_QUERY
    )
 
    captures = query.captures(root)
 
    symbols: List[Symbol] = []
    refs: List[Ref] = []
 
    name_index: Dict[str, List[str]] = {}
 
    for node, cap in captures:
        if cap.endswith("_decl"):
            name_node = node.child_by_field_name("name")
            if not name_node:
                continue
            name = node_text(src, name_node)
            rng = to_range(node)
 
            sid = stable_id("s", path, name, str(rng))
            symbols.append(Symbol(sid, cap.replace("_decl", ""), name, path, rng))
            name_index.setdefault(name, []).append(sid)
 
    for node, cap in captures:
        if cap == "call" or cap == "newexpr":
            name_node = node.child_by_field_name("name")
            if not name_node:
                continue
            name = node_text(src, name_node)
            rng = to_range(node)
 
            rid = stable_id("r", path, name, str(rng))
            refs.append(
                Ref(rid, "call", name, path, rng, name_index.get(name, []))
            )
 
    return {
        "language": lang,
        "hash": sha256_bytes(src),
        "symbols": [s.__dict__ for s in symbols],
        "refs": [r.__dict__ for r in refs],
    }, symbols
 
# -----------------------------
# Main
# -----------------------------
 
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="repo_ir.json")
    args = ap.parse_args()
 
    repo_ir = {
        "version": "0.3",
        "repo": {
            "name": os.path.basename(os.getcwd()),
            "commit": git(["rev-parse", "HEAD"]),
            "generated_at": datetime.utcnow().isoformat() + "Z",
        },
        "files": {},
        "symbols": {},
        "indexes": {"symbols_by_name": {}},
    }
 
    for path in iter_source_files("."):
        lang = detect_language(path)
        with open(path, "rb") as f:
            src = f.read()
 
        entry, symbols = extract_file(path, lang, src)
        norm = path.replace("\\", "/").lstrip("./")
        repo_ir["files"][norm] = entry
 
        for s in symbols:
            repo_ir["symbols"][s.id] = s.__dict__
            repo_ir["indexes"]["symbols_by_name"].setdefault(s.name, []).append(s.id)
 
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(repo_ir, f, indent=2)
 
    print(f"[repo-ir] files={len(repo_ir['files'])} symbols={len(repo_ir['symbols'])}")
 
if __name__ == "__main__":
    main()
