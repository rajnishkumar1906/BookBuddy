from fastapi import APIRouter, HTTPException ,Depends
from auth.schemas import LoginRequest, TokenResponse
from core.security import create_access_token
import bcrypt
from core.db import get_db

router = APIRouter()

@router.post("/login", response_model=TokenResponse)
def login(data: LoginRequest):
    # DEV login (replace later)
    cur.execute("SELECT password_hash FROM users WHERE email=%s", (data.email,))
    row = cur.fetchone()
    if not row or not bcrypt.checkpw(data.password.encode(), row[0].encode()):
        raise HTTPException(status_code=401)

    token = create_access_token({"sub": data.email})
    return {"access_token": token}

@router.post("/register")
def register(email: str, password: str, db=Depends(get_db)):
    hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()
    with db.cursor() as cur:
        cur.execute(
            "INSERT INTO users (email, password_hash) VALUES (%s, %s)",
            (email, hashed)
        )
        db.commit()
    return {"status": "user created"}

@router.post("/refresh")
def refresh(token: str, db=Depends(get_db)):
    cur.execute("SELECT user_id FROM refresh_tokens WHERE token=%s", (token,))
    
    
@router.get("/google/login")
def google_login():
    return oauth.google.authorize_redirect(...)

@router.get("/google/callback")
def google_callback():
    user_info = oauth.google.authorize_access_token()