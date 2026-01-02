from flask import Flask, render_template, request, redirect, url_for, make_response, flash, session
from werkzeug.security import generate_password_hash, check_password_hash
import hashlib
import json
from datetime import datetime, timedelta
import base64
import os
import secrets # Added for secure token generation
import re # Added for password policy validation

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', 'a_fallback_secret_key_for_dev_only_do_not_use_in_prod')
app.permanent_session_lifetime = timedelta(days=30)

# Storing user data in memory (Note: In-memory storage is not suitable for production)
users = {
    'admin': {
        'password': generate_password_hash('admin123'),
        'email': 'admin@example.com',
        'role': 'admin'
    },
    'user': {
        'password': generate_password_hash('password123'),
        'email': 'user@example.com',
        'role': 'user'
    }
}

# Storing reset tokens in memory (Note: In-memory storage is not suitable for production)
password_reset_tokens = {}

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/lab')
def lab():
    return render_template('lab.html')

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username')
    password = request.form.get('password')
    remember_me = request.form.get('remember_me')

    if username in users and check_password_hash(users[username]['password'], password):
        session['username'] = username
        if remember_me:
            session.permanent = True
        
        flash('Login successful!')
        return redirect(url_for('dashboard'))
    
    flash('Invalid username or password')
    return redirect(url_for('lab'))

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username')
    password = request.form.get('password')
    email = request.form.get('email')
    
    if username and password and email:
        # Remediation 4: Basic password complexity requirements
        if len(password) < 8 or not re.search(r"[a-zA-Z]", password) or not re.search(r"\d", password):
            flash('Password must be at least 8 characters long and contain both letters and digits.')
            return redirect(url_for('lab'))

        if username not in users:
            users[username] = {
                'password': generate_password_hash(password),
                'email': email,
                'role': 'user'
            }
            flash('Registration successful')
            return redirect(url_for('lab'))
    
    flash('Registration failed')
    return redirect(url_for('lab'))

@app.route('/reset-password', methods=['POST'])
def reset_password():
    email = request.form.get('email')
    
    for username, user_data in users.items():
        if user_data['email'] == email:
            # Remediation 1: Secure, random token generation
            token = secrets.token_urlsafe(32)
            password_reset_tokens[token] = username
            
            # Remediation 2: Simulate email send, do NOT expose token in UI
            flash('Password reset link sent to your email (simulated).')
            # In a real application, this would send an email like:
            # send_email(email, 'Password Reset', f'Click here to reset your password: {url_for("reset_form", token=token, _external=True)}')
            return redirect(url_for('lab'))
    
    flash('Email not found')
    return redirect(url_for('lab'))

@app.route('/reset/<token>')
def reset_form(token):
    if token in password_reset_tokens:
        return render_template('reset.html', token=token)
    return 'Invalid token'

@app.route('/dashboard')
def dashboard():
    if 'username' not in session:
        return redirect(url_for('lab'))
    
    username = session['username']
    if username in users:
        return render_template('dashboard.html', 
                                username=username, 
                                role=users[username]['role'],
                                email=users[username]['email'])
    
    return redirect(url_for('lab'))

if __name__ == '__main__':
    # Remediation 3: Gate debug mode on environment variable
    debug_mode = os.environ.get('FLASK_DEBUG', 'False') == 'True'
    app.run(host='0.0.0.0', port=5000, debug=debug_mode)
