package com.smartprocurement.module.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.application.dto.ApplicationCreateRequest;
import com.smartprocurement.module.application.dto.ApplicationItemRequest;
import com.smartprocurement.module.application.dto.ApplicationListRow;
import com.smartprocurement.module.application.dto.ApplicationResponse;
import com.smartprocurement.module.application.entity.ProcurementApplicationItem;
import com.smartprocurement.module.application.enums.ApplicationStatus;
import com.smartprocurement.module.application.mapper.ProcurementApplicationItemMapper;
import com.smartprocurement.module.application.mapper.ProcurementApplicationMapper;
import com.smartprocurement.module.department.entity.SysDepartment;
import com.smartprocurement.module.department.mapper.SysDepartmentMapper;
import com.smartprocurement.module.role.entity.SysRole;
import com.smartprocurement.module.role.mapper.SysRoleMapper;
import com.smartprocurement.module.user.entity.SysUser;
import com.smartprocurement.module.user.entity.SysUserRole;
import com.smartprocurement.module.user.mapper.SysUserMapper;
import com.smartprocurement.module.user.mapper.SysUserRoleMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购申请 Excel 导入导出。
 * 一个文件 = 一张采购申请：表头字段（标题/部门/审批人等）取自第一条明细行，
 * 后续每条有物料名称的行都是一条采购明细。
 */
@Service
public class ApplicationExcelService {

    /**
     * 实体模板文件在 classpath 下的位置；文件可直接用 Excel 打开手工维护，
     * 修改模板无需改动 Java 代码。
     */
    private static final String TEMPLATE_CLASSPATH = "templates/采购申请导入模板.xlsx";

    /**
     * 逻辑列键 → 可识别的表头别名。
     * 用户自制 Excel 时表头只要"包含"任一别名（忽略 *、空格、标点）即可识别，
     * 因此列顺序可任意调整，必填星号可写可不写。
     */
    private static final Map<String, String[]> HEADER_ALIASES = new LinkedHashMap<>();
    static {
        HEADER_ALIASES.put("title", new String[] { "申请标题", "标题" });
        HEADER_ALIASES.put("department", new String[] { "申请部门", "部门名称", "部门" });
        HEADER_ALIASES.put("approver", new String[] { "审批人" });
        HEADER_ALIASES.put("type", new String[] { "申请类型", "紧急标识", "类型" });
        HEADER_ALIASES.put("purpose", new String[] { "申请事由", "事由" });
        HEADER_ALIASES.put("requiredDate", new String[] { "期望到货日期", "期望到货" });
        HEADER_ALIASES.put("materialCode", new String[] { "物料编码", "物料编号" });
        HEADER_ALIASES.put("materialName", new String[] { "物料名称", "品名" });
        HEADER_ALIASES.put("specification", new String[] { "规格型号", "规格" });
        HEADER_ALIASES.put("unit", new String[] { "单位", "计量单位" });
        HEADER_ALIASES.put("quantity", new String[] { "申请数量", "数量" });
        HEADER_ALIASES.put("unitPrice", new String[] { "预估单价", "单价" });
        HEADER_ALIASES.put("lineDate", new String[] { "行到货日期" });
        HEADER_ALIASES.put("supplierCode", new String[] { "建议供应商编码", "供应商编码" });
        HEADER_ALIASES.put("supplierName", new String[] { "建议供应商名称", "供应商名称" });
        HEADER_ALIASES.put("remark", new String[] { "备注" });
    }

    /** 必填的逻辑列键 */
    private static final String[] REQUIRED_KEYS = { "title", "department", "approver", "materialName", "quantity" };

    private static final String[] EXPORT_HEADERS = {
            "申请单号", "状态", "申请标题", "申请部门", "申请人", "审批人", "申请类型", "申请事由",
            "金额合计", "期望到货日期", "创建时间",
            "物料编码", "物料名称", "规格型号", "单位", "数量", "预估单价", "预估金额", "行到货日期",
            "建议供应商编码", "建议供应商名称", "备注"
    };

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProcurementApplicationMapper applicationMapper;
    private final ProcurementApplicationItemMapper itemMapper;
    private final SysDepartmentMapper departmentMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final ApplicationService applicationService;

    public ApplicationExcelService(
            ProcurementApplicationMapper applicationMapper,
            ProcurementApplicationItemMapper itemMapper,
            SysDepartmentMapper departmentMapper,
            SysUserMapper userMapper,
            SysUserRoleMapper userRoleMapper,
            SysRoleMapper roleMapper,
            ApplicationService applicationService) {
        this.applicationMapper = applicationMapper;
        this.itemMapper = itemMapper;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.applicationService = applicationService;
    }

    // ── 模板下载 ──────────────────────────────────────────────────────

    /**
     * 直接返回 classpath 下的实体模板文件（src/main/resources/templates/采购申请导入模板.xlsx）。
     * 模板由人工用 Excel 维护，接口只负责把文件原样下发，不做任何加工。
     */
    public byte[] buildTemplate() {
        try (InputStream in = new ClassPathResource(TEMPLATE_CLASSPATH).getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("读取导入模板失败，请联系管理员检查模板文件");
        }
    }

    // ── 导入 ──────────────────────────────────────────────────────────

    public ApplicationResponse importApplications(MultipartFile file, Long applicantId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("导入文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !(originalName.endsWith(".xlsx") || originalName.endsWith(".xls"))) {
            throw new BusinessException("仅支持 .xlsx / .xls 格式的 Excel 文件");
        }

        try (InputStream in = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new BusinessException("Excel 没有可导入的数据行");
            }

            // 按表头名称定位列下标，允许列顺序与模板不完全一致
            Map<String, Integer> columnIndex = resolveHeaderIndex(sheet.getRow(0));

            // 表头字段取第一条有效数据行
            Row firstDataRow = null;
            List<Row> dataRows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String materialName = getString(row, columnIndex, "materialName");
                if (materialName == null || materialName.isBlank()) {
                    continue;
                }
                dataRows.add(row);
                if (firstDataRow == null) {
                    firstDataRow = row;
                }
            }
            if (firstDataRow == null) {
                throw new BusinessException("未找到有效的明细行（物料名称不能为空）");
            }

            String title = requireHeaderValue(firstDataRow, columnIndex, "title");
            String departmentName = requireHeaderValue(firstDataRow, columnIndex, "department");
            String approverName = requireHeaderValue(firstDataRow, columnIndex, "approver");
            String typeText = getString(firstDataRow, columnIndex, "type");
            String purpose = getString(firstDataRow, columnIndex, "purpose");
            LocalDate requiredDate = getDate(firstDataRow, columnIndex, "requiredDate");

            Long departmentId = resolveDepartmentId(departmentName);
            Long approverId = resolveApproverId(approverName);

            List<ApplicationItemRequest> items = new ArrayList<>();
            int lineNo = 1;
            for (Row row : dataRows) {
                String materialName = requireCell(row, columnIndex, "materialName", lineNo);
                BigDecimal quantity = getBigDecimal(row, columnIndex, "quantity", lineNo);
                if (quantity == null || quantity.signum() <= 0) {
                    throw new BusinessException("第" + (row.getRowNum() + 1) + "行数量必须大于0");
                }
                BigDecimal unitPrice = getBigDecimal(row, columnIndex, "unitPrice");
                // 模板不提供预估金额列，统一由 数量×单价 计算，保证口径一致
                BigDecimal amount = unitPrice == null ? null : quantity.multiply(unitPrice);

                items.add(new ApplicationItemRequest(
                        materialName,
                        getString(row, columnIndex, "materialCode"),
                        getString(row, columnIndex, "specification"),
                        getString(row, columnIndex, "unit"),
                        quantity,
                        unitPrice,
                        amount,
                        getDate(row, columnIndex, "lineDate"),
                        getString(row, columnIndex, "supplierCode"),
                        getString(row, columnIndex, "supplierName"),
                        getString(row, columnIndex, "remark")));
                lineNo++;
            }

            String applicationType = "紧急".equals(typeText) || "URGENT".equalsIgnoreCase(typeText)
                    ? "URGENT"
                    : "NORMAL";

            ApplicationCreateRequest request = new ApplicationCreateRequest(
                    title, departmentId, approverId, applicationType, purpose,
                    null, null, null, "CNY", requiredDate, items, null);
            return applicationService.createDraft(request, applicantId);

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Excel 解析失败：" + ex.getMessage());
        }
    }

    /**
     * 扫描 Excel 第一行表头，把物理列下标映射成逻辑列键。
     * 识别规则：表头文字归一化（去掉 *、空格、括号等标点）后，
     * 只要包含任一别名即命中，因此用户自制模板时列顺序、必填星号、
     * 括号里的格式提示都不会影响识别。
     */
    private Map<String, Integer> resolveHeaderIndex(Row headerRow) {
        if (headerRow == null) {
            throw new BusinessException("Excel 缺少表头，请使用系统提供的导入模板");
        }
        Map<String, Integer> index = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell.getCellType() != org.apache.poi.ss.usermodel.CellType.STRING) {
                continue;
            }
            String normalizedHeader = normalizeHeader(cell.getStringCellValue());
            if (normalizedHeader.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String[]> entry : HEADER_ALIASES.entrySet()) {
                String key = entry.getKey();
                if (index.containsKey(key)) {
                    continue;
                }
                for (String alias : entry.getValue()) {
                    if (normalizedHeader.contains(alias)) {
                        index.put(key, cell.getColumnIndex());
                        break;
                    }
                }
            }
        }
        for (String required : REQUIRED_KEYS) {
            if (!index.containsKey(required)) {
                throw new BusinessException("Excel 缺少必需列「" + HEADER_ALIASES.get(required)[0]
                        + "」，请使用系统提供的导入模板");
            }
        }
        return index;
    }

    /** 表头归一化：去掉所有空白和标点符号（*、括号、冒号等），只保留中英文、数字、斜杠 */
    private String normalizeHeader(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\s*＊()（）\\[\\]【】:：,，.。/\\-_]+", "")
                .replace("yyyyMMdd", "").replace("YYYYMMDD", "").trim();
    }

    private Long resolveDepartmentId(String name) {
        SysDepartment department = departmentMapper.selectOne(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getName, name)
                        .isNull(SysDepartment::getDeletedAt)
                        .last("limit 1"));
        if (department == null) {
            throw new BusinessException("申请部门「" + name + "」不存在，请与系统中的部门名称保持一致");
        }
        return department.getId();
    }

    private Long resolveApproverId(String name) {
        // 优先按姓名匹配，其次按登录账号匹配
        SysUser approver = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDisplayName, name)
                        .isNull(SysUser::getDeletedAt)
                        .last("limit 1"));
        if (approver == null) {
            approver = userMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, name)
                            .isNull(SysUser::getDeletedAt)
                            .last("limit 1"));
        }
        if (approver == null) {
            throw new BusinessException("审批人「" + name + "」不存在，请填写系统用户的姓名或登录账号");
        }

        // 校验该用户具备审批人或管理员角色
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, approver.getId()))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            throw new BusinessException("审批人「" + name + "」未分配任何角色，不能作为审批人");
        }
        List<String> codes = roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getCode).toList();
        if (!codes.contains("APPROVER") && !codes.contains("ADMIN")) {
            throw new BusinessException("用户「" + name + "」不具备审批人角色");
        }
        return approver.getId();
    }

    // ── 导出 ──────────────────────────────────────────────────────────

    public byte[] exportApplications(String keyword, String status) {
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<ApplicationListRow> pageParam = new Page<>(1, 10_000);
        List<ApplicationListRow> applications = applicationMapper
                .selectApplicationPage(pageParam, trimmedKeyword, status, null).getRecords();

        // 一次性查出所有申请的明细，避免逐行查询
        List<ProcurementApplicationItem> allItems = applications.isEmpty() ? List.of()
                : itemMapper.selectList(
                        new LambdaQueryWrapper<ProcurementApplicationItem>()
                                .in(ProcurementApplicationItem::getApplicationId,
                                        applications.stream().map(ApplicationListRow::getId).toList())
                                .isNull(ProcurementApplicationItem::getDeletedAt)
                                .orderByAsc(ProcurementApplicationItem::getApplicationId)
                                .orderByAsc(ProcurementApplicationItem::getLineNo));
        Map<Long, List<ProcurementApplicationItem>> itemMap = new HashMap<>();
        for (ProcurementApplicationItem item : allItems) {
            itemMap.computeIfAbsent(item.getApplicationId(), k -> new ArrayList<>()).add(item);
        }

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("采购申请");

            CellStyle headerStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            int rowIndex = 1;
            for (ApplicationListRow app : applications) {
                List<ProcurementApplicationItem> items = itemMap.getOrDefault(app.getId(), List.of());
                if (items.isEmpty()) {
                    writeExportRow(sheet.createRow(rowIndex++), app, null);
                    continue;
                }
                for (ProcurementApplicationItem item : items) {
                    writeExportRow(sheet.createRow(rowIndex++), app, item);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("导出 Excel 失败");
        }
    }

    private void writeExportRow(Row row, ApplicationListRow app, ProcurementApplicationItem item) {
        int c = 0;
        row.createCell(c++).setCellValue(nullToEmpty(app.getApplicationNo()));
        row.createCell(c++).setCellValue(statusText(app.getStatus()));
        row.createCell(c++).setCellValue(nullToEmpty(app.getTitle()));
        row.createCell(c++).setCellValue(nullToEmpty(app.getDepartmentName()));
        row.createCell(c++).setCellValue(nullToEmpty(app.getApplicantName()));
        row.createCell(c++).setCellValue(app.getApproverName() == null ? "未指定" : app.getApproverName());
        row.createCell(c++).setCellValue("URGENT".equals(app.getApplicationType()) ? "紧急" : "普通");
        row.createCell(c++).setCellValue(nullToEmpty(app.getPurpose()));
        setNumberCell(row.createCell(c++), app.getTotalAmount());
        row.createCell(c++).setCellValue(app.getRequiredDate() == null ? "" : app.getRequiredDate().toString());
        row.createCell(c++).setCellValue(app.getCreatedAt() == null ? "" : app.getCreatedAt().format(DATE_TIME_FORMAT));

        if (item == null) {
            return;
        }
        row.createCell(c++).setCellValue(nullToEmpty(item.getMaterialCode()));
        row.createCell(c++).setCellValue(nullToEmpty(item.getMaterialName()));
        row.createCell(c++).setCellValue(nullToEmpty(item.getSpecification()));
        row.createCell(c++).setCellValue(nullToEmpty(item.getUnit()));
        setNumberCell(row.createCell(c++), item.getQuantity());
        setNumberCell(row.createCell(c++), item.getEstimatedUnitPrice());
        setNumberCell(row.createCell(c++), item.getEstimatedAmount());
        row.createCell(c++).setCellValue(item.getRequiredDate() == null ? "" : item.getRequiredDate().toString());
        row.createCell(c++).setCellValue(nullToEmpty(item.getSuggestSupplierCode()));
        row.createCell(c++).setCellValue(nullToEmpty(item.getSuggestSupplierName()));
        row.createCell(c).setCellValue(nullToEmpty(item.getRemark()));
    }

    private String statusText(String code) {
        if (code == null) {
            return "";
        }
        try {
            return switch (ApplicationStatus.valueOf(code)) {
                case DRAFT -> "草稿";
                case SUBMITTED -> "已提交";
                case VALIDATING -> "校验中";
                case PENDING_APPROVAL -> "待审批";
                case APPROVED -> "已通过";
                case REJECTED -> "已驳回";
                case WITHDRAWN -> "已撤回";
            };
        } catch (IllegalArgumentException ex) {
            return code;
        }
    }

    // ── 单元格读取工具 ────────────────────────────────────────────────

    private String requireHeaderValue(Row row, Map<String, Integer> index, String key) {
        String value = getString(row, index, key);
        if (value == null || value.isBlank()) {
            throw new BusinessException("「" + HEADER_ALIASES.get(key)[0] + "」不能为空");
        }
        return value;
    }

    private String requireCell(Row row, Map<String, Integer> index, String key, int lineNo) {
        String value = getString(row, index, key);
        if (value == null || value.isBlank()) {
            throw new BusinessException("第" + (lineNo + 1) + "条明细的「"
                    + HEADER_ALIASES.get(key)[0] + "」不能为空");
        }
        return value;
    }

    private String getString(Row row, Map<String, Integer> index, String header) {
        Integer col = index.get(header);
        if (col == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : trimDouble(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield trimDouble(cell.getNumericCellValue());
                } catch (IllegalStateException ex) {
                    yield cell.getStringCellValue().trim();
                }
            }
            default -> null;
        };
    }

    private BigDecimal getBigDecimal(Row row, Map<String, Integer> index, String header) {
        return getBigDecimal(row, index, header, -1);
    }

    private BigDecimal getBigDecimal(Row row, Map<String, Integer> index, String header, int lineNo) {
        Integer col = index.get(header);
        if (col == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            String text = getString(row, index, header);
            if (text == null || text.isBlank()) {
                return null;
            }
            return new BigDecimal(text.replace(",", ""));
        } catch (NumberFormatException ex) {
            String where = lineNo > 0 ? "第" + (lineNo + 1) + "条明细的" : "";
            throw new BusinessException(where + "「" + HEADER_ALIASES.get(header)[0] + "」不是合法数字：" + cell);
        }
    }

    private LocalDate getDate(Row row, Map<String, Integer> index, String header) {
        Integer col = index.get(header);
        if (col == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = getString(row, index, header);
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.replace("/", "-").trim();
        try {
            return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text);
        } catch (Exception ex) {
            throw new BusinessException("日期「" + text + "」格式不正确，请使用 yyyy-MM-dd");
        }
    }

    private String trimDouble(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private void setNumberCell(Cell cell, BigDecimal value) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
