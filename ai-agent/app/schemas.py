from decimal import Decimal
from typing import Literal, Optional

from pydantic import BaseModel, Field


class ApplicationItem(BaseModel):
    material_name: str
    material_code: Optional[str] = None
    specification: Optional[str] = None
    unit: Optional[str] = None
    quantity: Decimal = Field(gt=0)
    estimated_amount: Optional[Decimal] = None


class ApplicationValidationRequest(BaseModel):
    application_no: str
    title: str
    application_type: str = "NORMAL"
    purpose: Optional[str] = None
    budget_available_amount: Optional[Decimal] = None
    total_amount: Decimal = Field(ge=0)
    items: list[ApplicationItem]


class ValidationItem(BaseModel):
    item_code: str
    item_name: str
    result: Literal["PASS", "NEED_REVIEW", "REJECT", "NA"]
    risk_level: Literal["LOW", "MEDIUM", "HIGH"]
    description: str
    suggestion: Optional[str] = None


class ApplicationValidationResult(BaseModel):
    application_no: str
    overall_risk_level: Literal["LOW", "MEDIUM", "HIGH"]
    overall_result: Literal["PASS", "NEED_REVIEW", "REJECT"]
    summary: str
    suggestion: str
    confidence: float = Field(ge=0, le=1)
    items: list[ValidationItem]
