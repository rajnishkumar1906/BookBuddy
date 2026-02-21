from fastapi import APIRouter, HTTPException, Depends, Request
from fastapi.responses import RedirectResponse
from auth.schemas import LoginRequest, RegisterRequest, RefreshRequest, TokenResponse
from core.security import create_access_token
from core.db import get_db
from core.config import settings

from authlib.integrations.starlette_client import OAuth
import bcrypt
import secrets
from datetime import datetime, timedelta


router = APIRouter()

# =====================================================
# OAuth (Google)
# =====================================================
oauth = OAuth()

oauth.register(
    name="google",
    client_id=settings.GOOGLE_CLIENT_ID,
    client_secret=settings.GOOGLE_CLIENT_SECRET,
    server_metadata_url="https://accounts.google.com/.well-known/openid-configuration",
    client_kwargs={"scope": "openid email profile"},
)

# =====================================================
# LOGIN
# =====================================================
@router.post("/login", response_model=TokenResponse)
def login(data: LoginRequest, db=Depends(get_db)):
    with db.cursor() as cur:
        cur.execute(
            "SELECT id, password_hash FROM users WHERE email=%s",
            (data.email,),
        )
        row = cur.fetchone()

    if not row:
        raise HTTPException(status_code=401, detail="Invalid credentials")

    user_id, password_hash = row

    if password_hash == "GOOGLE_OAUTH":
        raise HTTPException(
            status_code=400,
            detail="This account uses Google login",
        )

    if not bcrypt.checkpw(data.password.encode(), password_hash.encode()):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    access_token = create_access_token({"sub": data.email})

    refresh_token = secrets.token_urlsafe(32)
    expires_at = datetime.utcnow() + timedelta(days=7)

    with db.cursor() as cur:
        cur.execute(
            """
            INSERT INTO refresh_tokens (token, user_id, expires_at)
            VALUES (%s, %s, %s)
            """,
            (refresh_token, user_id, expires_at),
        )
        db.commit()

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
    }

# =====================================================
# REGISTER
# =====================================================
@router.post("/register")
def register(data: RegisterRequest, db=Depends(get_db)):
    hashed = bcrypt.hashpw(data.password.encode(), bcrypt.gensalt()).decode()

    with db.cursor() as cur:
        cur.execute(
            "SELECT 1 FROM users WHERE email=%s",
            (data.email,),
        )
        if cur.fetchone():
            raise HTTPException(
                status_code=400,
                detail="Email already registered",
            )

        cur.execute(
            "INSERT INTO users (email, password_hash) VALUES (%s, %s)",
            (data.email, hashed),
        )
        db.commit()

    return {"status": "user created"}

# =====================================================
# REFRESH TOKEN
# =====================================================
@router.post("/refresh")
def refresh(data: RefreshRequest, db=Depends(get_db)):
    with db.cursor() as cur:
        cur.execute(
            """
            SELECT u.email
            FROM refresh_tokens r
            JOIN users u ON r.user_id = u.id
            WHERE r.token = %s AND r.expires_at > NOW()
            """,
            (data.refresh_token,),
        )
        row = cur.fetchone()

    if not row:
        raise HTTPException(status_code=401, detail="Invalid refresh token")

    email = row[0]
    new_access_token = create_access_token({"sub": email})

    return {
        "access_token": new_access_token,
        "token_type": "bearer",
    }

@router.get("/google/login")
async def google_login(request: Request):
    print("GOOGLE_CLIENT_ID:", settings.GOOGLE_CLIENT_ID)
    print("GOOGLE_CLIENT_SECRET:", settings.GOOGLE_CLIENT_SECRET[:5] + "..." if settings.GOOGLE_CLIENT_SECRET else "MISSING")
    print("GOOGLE_REDIRECT_URI:", settings.GOOGLE_REDIRECT_URI)

    if not settings.GOOGLE_REDIRECT_URI:
        raise HTTPException(status_code=500, detail="GOOGLE_REDIRECT_URI not configured")

    try:
        return await oauth.google.authorize_redirect(
            request,
            redirect_uri=settings.GOOGLE_REDIRECT_URI,
        )
    except Exception as e:
        print("OAuth redirect error:", str(e))
        raise HTTPException(status_code=500, detail=f"OAuth error: {str(e)}")

# GOOGLE CALLBACK - handle redirect from Google
@router.get("/google/callback")
async def google_callback(request: Request, db=Depends(get_db)):
    token = await oauth.google.authorize_access_token(request)  # ← await here too!
    user_info = token["userinfo"]

    email = user_info["email"]

    with db.cursor() as cur:
        cur.execute("SELECT id FROM users WHERE email=%s", (email,))
        row = cur.fetchone()

        if row:
            user_id = row[0]
        else:
            cur.execute(
                """
                INSERT INTO users (email, password_hash)
                VALUES (%s, %s)
                RETURNING id
                """,
                (email, "GOOGLE_OAUTH"),
            )
            user_id = cur.fetchone()[0]
            db.commit()

    access_token = create_access_token({"sub": email})

    refresh_token = secrets.token_urlsafe(32)
    expires_at = datetime.utcnow() + timedelta(days=7)

    with db.cursor() as cur:
        cur.execute(
            """
            INSERT INTO refresh_tokens (token, user_id, expires_at)
            VALUES (%s, %s, %s)
            """,
            (refresh_token, user_id, expires_at),
        )
        db.commit()

    # Redirect to frontend
    frontend_url = settings.FRONTEND_URL
    redirect_url = f"{frontend_url}/?access_token={access_token}&refresh_token={refresh_token}"
    return RedirectResponse(url=redirect_url)