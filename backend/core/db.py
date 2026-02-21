import psycopg # type: ignore
from core.config import settings

def get_db():
    conn = psycopg.connect(settings.DATABASE_URL)
    try:
        yield conn
    finally:
        conn.close()