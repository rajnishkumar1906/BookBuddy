from fastapi import APIRouter, Depends, HTTPException, Query
from typing import List
from books.schemas import Book
from retrieval.supabase_fetch import fetch_books_by_ids
from core.db import get_db
from core.security import require_role

router = APIRouter()


# --------------------------------------------------
# Get single book by ID
# --------------------------------------------------
@router.get("/{book_id}", response_model=Book)
def get_book(book_id: str):
    books = fetch_books_by_ids([book_id])
    if not books:
        raise HTTPException(status_code=404, detail="Book not found")
    return books[0]


# --------------------------------------------------
# Create book (ADMIN ONLY)
# --------------------------------------------------
@router.post("/", dependencies=[Depends(require_role("admin"))])
def create_book(book: Book, db=Depends(get_db)):
    with db.cursor() as cur:
        cur.execute(
            """
            INSERT INTO books
            (book_id, book_title, author, genres, book_details, num_pages, cover_image_url)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            """,
            (
                book.book_id,
                book.title,
                book.author,
                book.genres,
                book.description,
                book.num_pages,
                book.image_url,
            ),
        )
        db.commit()

    return {"status": "book created", "book_id": book.book_id}


# --------------------------------------------------
# Update book (ADMIN ONLY)
# --------------------------------------------------
@router.put("/{book_id}", dependencies=[Depends(require_role("admin"))])
def update_book(book_id: str, book: Book, db=Depends(get_db)):
    with db.cursor() as cur:
        cur.execute(
            """
            UPDATE books
            SET
                book_title = %s,
                author = %s,
                genres = %s,
                book_details = %s,
                num_pages = %s,
                cover_image_url = %s
            WHERE book_id = %s
            """,
            (
                book.title,
                book.author,
                book.genres,
                book.description,
                book.num_pages,
                book.image_url,
                book_id,
            ),
        )

        if cur.rowcount == 0:
            raise HTTPException(status_code=404, detail="Book not found")

        db.commit()

    return {"status": "book updated", "book_id": book_id}


# --------------------------------------------------
# List books with pagination
# --------------------------------------------------
@router.get("/", response_model=List[Book])
def list_books(
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    db=Depends(get_db),
):
    offset = (page - 1) * limit

    with db.cursor() as cur:
        cur.execute(
            """
            SELECT
                book_id,
                book_title,
                author,
                genres,
                book_details,
                num_pages,
                cover_image_url
            FROM books
            ORDER BY book_id
            LIMIT %s OFFSET %s
            """,
            (limit, offset),
        )
        rows = cur.fetchall()

    return [
        {
            "book_id": r[0],
            "title": r[1],
            "author": r[2],
            "genres": r[3],
            "description": r[4],
            "num_pages": r[5],
            "image_url": r[6],
        }
        for r in rows
    ]


# --------------------------------------------------
# Browse books by genre
# --------------------------------------------------
@router.get("/by-genre/{genre}", response_model=List[Book])
def by_genre(
    genre: str,
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    db=Depends(get_db),
):
    offset = (page - 1) * limit

    with db.cursor() as cur:
        cur.execute(
            """
            SELECT
                book_id,
                book_title,
                author,
                genres,
                book_details,
                num_pages,
                cover_image_url
            FROM books
            WHERE genres ILIKE %s
            ORDER BY book_id
            LIMIT %s OFFSET %s
            """,
            (f"%{genre}%", limit, offset),
        )
        rows = cur.fetchall()

    return [
        {
            "book_id": r[0],
            "title": r[1],
            "author": r[2],
            "genres": r[3],
            "description": r[4],
            "num_pages": r[5],
            "image_url": r[6],
        }
        for r in rows
    ]