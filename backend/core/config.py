# from pydantic_settings import BaseSettings

# class Settings(BaseSettings):
#     DATABASE_URL: str
#     JWT_SECRET: str = "dev-secret"
#     JWT_ALGORITHM: str = "HS256"
#     ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
#     # Google OAuth (optional – set in .env for Google login)
#     GOOGLE_CLIENT_ID: str = ""
#     GOOGLE_CLIENT_SECRET: str = ""
#     GOOGLE_REDIRECT_URI: str = ""
#     FRONTEND_URL: str = "http://localhost:5173"
#     # Gemini LLM (set GEMINI_API_KEY in .env)
#     GEMINI_API_KEY: str = ""

# settings = Settings()

from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    DATABASE_URL: str
    JWT_SECRET: str = "dev-secret"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60

    GOOGLE_CLIENT_ID: str = ""
    GOOGLE_CLIENT_SECRET: str = ""
    GOOGLE_REDIRECT_URI: str = ""
    FRONTEND_URL: str = "http://localhost:5173"
    GEMINI_API_KEY: str = ""

    model_config = SettingsConfigDict(
        env_file=".env",               # looks in current working directory
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )


settings = Settings()