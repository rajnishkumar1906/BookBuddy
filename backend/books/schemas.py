from pydantic import BaseModel

class Book(BaseModel):
    book_id: str
    title: str
    author: str
    genres: str
    num_pages: int
    image_url: str | None = None