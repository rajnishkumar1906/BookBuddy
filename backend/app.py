from fastapi import FastAPI
from auth.router import router as auth_router
from books.router import router as books_router
from assistant.router import router as assistant_router
from users.router import router as user_router

app = FastAPI(title="BookBuddy API")

app.include_router(auth_router, prefix="/auth", tags=["Auth"])
app.include_router(user_router, prefix="/users", tags=["Users"])
app.include_router(books_router, prefix="/books", tags=["Books"])
app.include_router(assistant_router, prefix="/assistant", tags=["Assistant"])


@app.get("/")
def root():
    return {"status": "BookBuddy backend running"}