from typing import List, Callable, Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import decode_token
from app.models.user import User, UserRole
from app.models.student import Student
from app.models.recruiter import Recruiter

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/login")


async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: AsyncSession = Depends(get_db)
) -> User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials or token expired.",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = decode_token(token)
        user_id: str = payload.get("sub")
        token_type: str = payload.get("type")
        if user_id is None or token_type != "access":
            raise credentials_exception
    except Exception:
        raise credentials_exception

    stmt = select(User).where(User.id == user_id)
    result = await db.execute(stmt)
    user = result.scalar_one_or_none()

    if user is None:
        raise credentials_exception
    return user


async def get_current_active_user(
    current_user: User = Depends(get_current_user)
) -> User:
    if not current_user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is inactive or disabled."
        )
    return current_user


def require_roles(*allowed_roles: UserRole) -> Callable:
    """RBAC dependency checking if user's role is in allowed_roles."""
    allowed_values = [role.value if isinstance(role, UserRole) else role for role in allowed_roles]

    async def role_checker(current_user: User = Depends(get_current_active_user)) -> User:
        if current_user.role not in allowed_values and current_user.role != UserRole.SUPER_ADMIN.value:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Access denied. Required role(s): {', '.join(allowed_values)}."
            )
        return current_user

    return role_checker


async def get_current_student(
    current_user: User = Depends(require_roles(UserRole.STUDENT)),
    db: AsyncSession = Depends(get_db)
) -> Student:
    stmt = select(Student).where(Student.user_id == current_user.id)
    result = await db.execute(stmt)
    student = result.scalar_one_or_none()
    if not student:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Student profile not found for this user account."
        )
    return student


async def get_current_recruiter(
    current_user: User = Depends(require_roles(UserRole.RECRUITER)),
    db: AsyncSession = Depends(get_db)
) -> Recruiter:
    stmt = select(Recruiter).where(Recruiter.user_id == current_user.id)
    result = await db.execute(stmt)
    recruiter = result.scalar_one_or_none()
    if not recruiter:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Recruiter profile not found for this user account."
        )
    return recruiter
