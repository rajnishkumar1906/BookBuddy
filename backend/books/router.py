from fastapi import APIRouter , Depends
from retrieval.supabase_fetch import fetch_books_by_ids
from books.schemas import Book

router = APIRouter()

@router.get("/{book_id}")
def get_book(book_id: str):
    books = fetch_books_by_ids([book_id])
    return books[0] if books else {}

@router.post("/", dependencies=[Depends(require_role("admin"))])
def create_book(book: Book, db=Depends(get_db)):
    ...
    
@router.put("/{book_id}", dependencies=[Depends(require_role("admin"))])
def update_book(book_id: str, book: Book, db=Depends(get_db)):
    ...
    
@router.get("/")
def list_books(page: int = 1, limit: int = 20):
    offset = (page - 1) * limit
    
@router.get("/by-genre/{genre}")
def by_genre(genre: str):
    SELECT * FROM books WHERE genres ILIKE %genre%