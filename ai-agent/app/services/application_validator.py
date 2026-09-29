from decimal import Decimal

from app.schemas import ApplicationValidationRequest, ApplicationValidationResult, ValidationItem


HIGH_AMOUNT_THRESHOLD = Decimal("100000")


def validate_application(request: ApplicationValidationRequest) -> ApplicationValidationResult:
    items: list[ValidationItem] = []
    high_risk_count = 0
    medium_risk_count = 0

    if not request.items:
        items.append(
            ValidationItem(
                item_code="COMPLETENESS",
                item_name="完整性校验",
                result="REJECT",
                risk_level="HIGH",
                description="采购明细为空",
                suggestion="请至少添加一条采购明细"
            )
        )
        high_risk_count += 1
    else:
        items.append(
            ValidationItem(
                item_code="COMPLETENESS",
                item_name="完整性校验",
                result="PASS",
                risk_level="LOW",
                description="采购明细完整"
            )
        )

    if request.budget_available_amount is not None and request.total_amount > request.budget_available_amount:
        shortage = request.total_amount - request.budget_available_amount
        items.append(
            ValidationItem(
                item_code="BUDGET_ENOUGH",
                item_name="预算充足性校验",
                result="REJECT",
                risk_level="HIGH",
                description=f"预算不足，缺口 {shortage} 元",
                suggestion="请补充预算或调整采购数量"
            )
        )
        high_risk_count += 1
    else:
        items.append(
            ValidationItem(
                item_code="BUDGET_ENOUGH",
                item_name="预算充足性校验",
                result="PASS",
                risk_level="LOW",
                description="预算充足或未提供预算数据"
            )
        )

    if request.total_amount >= HIGH_AMOUNT_THRESHOLD:
        items.append(
            ValidationItem(
                item_code="AMOUNT_LEVEL",
                item_name="金额等级校验",
                result="NEED_REVIEW",
                risk_level="HIGH",
                description=f"申请金额达到 {request.total_amount} 元，需重点审批",
                suggestion="建议升级审批层级"
            )
        )
        high_risk_count += 1
    else:
        items.append(
            ValidationItem(
                item_code="AMOUNT_LEVEL",
                item_name="金额等级校验",
                result="PASS",
                risk_level="LOW",
                description="申请金额处于普通审批范围"
            )
        )

    if request.application_type == "URGENT":
        items.append(
            ValidationItem(
                item_code="URGENCY",
                item_name="紧急程度校验",
                result="NEED_REVIEW",
                risk_level="MEDIUM",
                description="该申请为紧急采购",
                suggestion="审批人需确认紧急原因"
            )
        )
        medium_risk_count += 1

    missing_spec_count = sum(
        1 for item in request.items if not item.material_code or not item.specification
    )
    if missing_spec_count:
        items.append(
            ValidationItem(
                item_code="MATERIAL_QUALIFIED",
                item_name="物料合规校验",
                result="NEED_REVIEW",
                risk_level="MEDIUM",
                description=f"有 {missing_spec_count} 条物料缺少物料编码或规格",
                suggestion="建议补充物料编码和规格，避免后续验收争议"
            )
        )
        medium_risk_count += 1
    else:
        items.append(
            ValidationItem(
                item_code="MATERIAL_QUALIFIED",
                item_name="物料合规校验",
                result="PASS",
                risk_level="LOW",
                description="物料编码和规格完整"
            )
        )

    if high_risk_count:
        overall_risk_level = "HIGH"
        overall_result = "REJECT"
        summary = f"发现 {high_risk_count} 项高风险问题"
        suggestion = "建议驳回或要求申请人补充材料后重新提交"
    elif medium_risk_count:
        overall_risk_level = "MEDIUM"
        overall_result = "NEED_REVIEW"
        summary = f"发现 {medium_risk_count} 项需要人工关注的问题"
        suggestion = "建议审批人重点审核风险项"
    else:
        overall_risk_level = "LOW"
        overall_result = "PASS"
        summary = "未发现明显风险"
        suggestion = "可进入人工审批流程"

    confidence = 0.92 if overall_risk_level != "HIGH" else 0.96

    return ApplicationValidationResult(
        application_no=request.application_no,
        overall_risk_level=overall_risk_level,
        overall_result=overall_result,
        summary=summary,
        suggestion=suggestion,
        confidence=confidence,
        items=items
    )
