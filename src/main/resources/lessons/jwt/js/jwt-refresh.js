"use strict";

const jwt = require('jsonwebtoken');
const express = require('express');
const router = express.Router();

/**
 * Load JWT secret from a secure configuration source.
 * No hard-coded fallback is used to avoid CWE-798 (hard-coded credentials).
 */
const SECRET = process.env.JWT_SECRET;

if (!SECRET || typeof SECRET !== 'string' || SECRET.trim().length === 0) {
    // Fail fast on startup if the secret is not configured securely
    // This avoids silently using an insecure default.
    throw new Error('JWT secret is not configured. Please set the JWT_SECRET environment variable.');
}

router.post('/refresh', function (req, res) {
    const refreshToken = req.body && req.body.refreshToken;

    if (!refreshToken || typeof refreshToken !== 'string') {
        return res.status(400).send({ error: 'No refresh token provided' });
    }

    jwt.verify(refreshToken, SECRET, function (err, decoded) {
        if (err) {
            // Do not leak detailed JWT verification internals to the client
            return res.status(401).send({ error: 'Invalid refresh token' });
        }

        const newToken = jwt.sign({ user: decoded.user }, SECRET, { expiresIn: '15m' });
        res.send({ token: newToken });
    });
});

module.exports = router;
