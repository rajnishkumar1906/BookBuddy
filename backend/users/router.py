from fastapi import APIRouter, Depends
from core.security import get_current_user

router = APIRouter()

@router.get("/me")
def me(user: str = Depends(get_current_user)):
    return {"email": user}