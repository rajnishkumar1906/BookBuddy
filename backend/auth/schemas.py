from pydantic import BaseModel

class LoginRequest(BaseModel):
    email : str
    password : str
    
class Tokenresponse(BaseModel):
    access_token  : str
    token_type : str = 'bearer'
    