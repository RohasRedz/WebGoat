import argparse
import hashlib
import json
import os
import subprocess
from dataclasses import dataclass
from datetime import datetime
from typing import Dict, List, Optional, Tuple
 
from tree_sitter_languages import get_parser
 
 
# ----------------------------
# Configuration
# ----------------------------
 
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
    "scripts",        # <-- explicitly excluded
    "node_modules",
    "build",
    "dist",
    "target",
    ".gradle",
    ".idea",
    "__pycache__",
}
 
# -----------------------------
# Tree-sitter queries (minimal)
# -----------------------------
 
JAVA_QUERY = r"""
(package_declaration (scoped_identifier) @package)
(import_declaration (scoped_identifier) @import)
 
(class_declaration name: (identifier) @class_name) @class_decl
 
(method_declaration
  name: (identifier) @method_name
  parameters: (formal_parameters) @method_params
) @method_decl
 
(constructor_declaration
  name: (identifier) @ctor_name
  parameters: (formal_parameters) @ctor_params
) @ctor_decl
 
(method_invocation name: (identifier) @call_name) @call
(object_creation_expression type: (type_identifier) @new_type) @newexpr
(type_identifier) @type_use
"""
 
PY_QUERY = r"""
(import_statement name: (dotted_name) @import)
(import_from_statement module_name: (dotted_name) @from_module)
(import_from_statement name: (dotted_name) @from_name)
 
(class_definition name: (identifier) @class_name) @class_decl
(function_definition
  name: (identifier) @func_name
  parameters: (parameters) @func_params
) @func_decl
 
(call function: (identifier) @call_name) @call_simple
(call function: (attribute attribute: (identifier) @call_attr)) @call_attr
"""
 
JS_QUERY = r"""
(import_statement source: (string) @import_src)
 
(class_declaration name: (identifier) @class_name) @class_decl
(function_declaration name: (identifier) @func_name) @func_decl
(method_definition name: (property_identifier) @method_name) @method_decl
 
(call_expression function: (identifier) @call_name) @call_simple
(call_expression function: (member_expression property: (property_identifier) @call_prop)) @call_member
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
    files = []
    for dirpath, dirnames, filenames in os.walk(root):
        if should_exclude_dir(dirpath):
            dirnames[:] = []
            continue
        for f in filenames:
            full = os.path.join(dirpath, f)
            if detect_language(full):
                files.append(full)
    return files
 
# -----------------------------
# Data models
# -----------------------------
 
@dataclass
class Symbol:
    id: str
    kind: str
    name: str
    qname: str
    file: str
    range: List[int]
    container: Optional[str]
    arity: Optional[int]
 
@dataclass
class Ref:
    id: str
    kind: str
    name: str
    file: str
    range: List[int]
    in_symbol: Optional[str]
    candidates: List[str]
 
# -----------------------------
# Extraction
# -----------------------------
 
def extract_file(path: str, lang: str, src: bytes):
    parser = get_parser(lang)
    tree = parser.parse(src)
    root = tree.root_node
 
    query = root.language.query(
        JAVA_QUERY if lang == "java"
        else PY_QUERY if lang == "python"
        else JS_QUERY
    )
 
    captures = query.captures(root)
 
    symbols: List[Symbol] = []
    refs: List[Ref] = []
    sym_spans: List[Tuple[List[int], str]] = []
 
    def enclosing_symbol(rng):
        for srng, sid in sym_spans:
            if srng[0] <= rng[0] and srng[2] >= rng[2]:
                return sid
        return None
 
    for node, cap in captures:
        if cap in ("class_decl", "method_decl", "func_decl", "ctor_decl"):
            name = None
            params = None
 
            for c in node.children:
                if c.type in ("identifier", "property_identifier"):
                    name = node_text(src, c)
                if c.type in ("formal_parameters", "parameters"):
                    params = node_text(src, c)
 
            kind = (
                "class" if cap == "class_decl"
                else "constructor" if cap == "ctor_decl"
                else "method" if cap == "method_decl"
                else "function"
            )
 
            arity = params.count(",") + 1 if params and params != "()" else 0
            rng = to_range(node)
            qname = f"{path}:{name}"
 
            sid = stable_id("s", path, kind, name or "", str(rng))
            sym = Symbol(sid, kind, name or "<anon>", qname, path, rng, None, arity)
 
            symbols.append(sym)
            sym_spans.append((rng, sid))
 
    name_index = {}
    for s in symbols:
        name_index.setdefault(s.name, []).append(s.id)
 
    for node, cap in captures:
        if cap.startswith("call") or cap in ("newexpr", "type_use"):
            name = None
            for c in node.children:
                if c.type in ("identifier", "property_identifier", "type_identifier"):
                    name = node_text(src, c)
                    break
            if not name:
                continue
 
            rng = to_range(node)
            rid = stable_id("r", path, name, str(rng))
            refs.append(
                Ref(
                    rid,
                    "call" if cap.startswith("call") else "type",
                    name,
                    path,
                    rng,
                    enclosing_symbol(rng),
                    name_index.get(name, []),
                )
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
 
    commit = git(["rev-parse", "HEAD"])
    repo = os.path.basename(os.getcwd())
 
    files_ir = {}
    global_symbols = {}
    symbols_by_name = {}
 
    for path in iter_source_files("."):
        lang = detect_language(path)
        with open(path, "rb") as f:
            src = f.read()
 
        entry, symbols = extract_file(path, lang, src)
        norm = path.replace("\\", "/").lstrip("./")
        entry["path"] = norm
        files_ir[norm] = entry
 
        for s in symbols:
            global_symbols[s.id] = s.__dict__
            symbols_by_name.setdefault(s.name, []).append(s.id)
 
    repo_ir = {
        "version": "0.2",
        "repo": {
            "name": repo,
            "commit": commit,
            "generated_at": datetime.utcnow().isoformat() + "Z",
        },
        "indexes": {
            "symbols_by_name": symbols_by_name,
        },
        "symbols": global_symbols,
        "files": files_ir,
    }
 
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(repo_ir, f, indent=2)
 
    print(f"[repo-ir] files={len(files_ir)} symbols={len(global_symbols)}")
 
if __name__ == "__main__":
    main()
