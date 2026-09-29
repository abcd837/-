from fastapi import FastAPI

from app.schemas import ApplicationValidationRequest, ApplicationValidationResult
from app.services.application_validator import validate_application

app = FastAPI(
    title="智能采购 AI Agent",
    version="0.1.0"
)


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/api/v1/agents/application-validation", response_model=ApplicationValidationResult)
def application_validation(request: ApplicationValidationRequest) -> ApplicationValidationResult:
    return validate_application(request)
