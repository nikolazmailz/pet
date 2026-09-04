package ru.ntdev.srhr.common.audit.sender;

import org.junit.jupiter.api.Test;
import ru.ntdev.srhr.common.audit.sender.payavg.PayavgCalculateSender;
import ru.ntdev.srhr.common.audit.sender.payavg.PayavgEventSender;
import ru.ntdev.srhr.common.audit.sender.paystub.PaystubBodySender;
import ru.ntdev.srhr.common.audit.sender.paystub.PaystubHeadSender;
import ru.ntdev.srhr.common.audit.sender.paystub.PaystubMailSender;
import ru.ntdev.srhr.common.audit.sender.paystub.PaystubPdfSender;
import ru.ntdev.srhr.common.audit.sender.paystub.PaystubPeriodSender;
import ru.ntdev.srhr.common.audit.sender.requisition.CollectPendingCandidatesSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionAddCommentSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionCandidateActionSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionCandidateInfoSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionCandidateRecommendationSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionCandidateResumeSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionDelegationsSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionDictSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionGetInterviewersSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionInterviewersActionSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionOrgUnitsAllowableSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionPostListSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionStaffSearchSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionUiStateSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionUserInfoSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionVacancyCandidatesSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionVacancyDetailsSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionVacancyInfoSender;
import ru.ntdev.srhr.common.audit.sender.requisition.RequisitionVacancyStatisticsSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangePostDelegationSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangePostReportTaskSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangePostStaffDetailSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangePostStaffListEventSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangePostStaffListSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeReferenceCitySender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeReferenceDelegationSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeReferenceRefSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeReferenceStaffPositionSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeReferenceTabSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeReferenceTrmSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeReportAttachmentSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeReportTaskStatusSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeStaffDelegationListSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeStaffDependencyExpSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeStaffDependencySender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeStaffDetailSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeStaffListDetailSender;
import ru.ntdev.srhr.common.audit.sender.salarychange.SalarychangeStaffListSender;
import ru.ntdev.srhr.common.audit.sender.scheduleholiday.ScheduleHolidayPostRequestSender;
import ru.ntdev.srhr.common.audit.sender.scheduleholiday.ScheduleHolidayPostVacationLimitSender;
import ru.ntdev.srhr.common.audit.sender.scheduleholiday.ScheduleHolidayRefSender;
import ru.ntdev.srhr.common.audit.sender.scheduleholiday.ScheduleHolidayRequestInfoSender;
import ru.ntdev.srhr.common.audit.sender.scheduleholiday.ScheduleHolidayRequestSender;
import ru.ntdev.srhr.common.audit.sender.scheduleholiday.ScheduleHolidayUserSender;
import ru.ntdev.srhr.common.audit.sender.teamcalendar.TeamCalendarGetRequestResolutionSender;
import ru.ntdev.srhr.common.audit.sender.teamcalendar.TeamCalendarOrgUnitEmployeeSender;
import ru.ntdev.srhr.common.audit.sender.teamcalendar.TeamCalendarOrgUnitSender;
import ru.ntdev.srhr.common.audit.sender.teamcalendar.TeamCalendarPageReferenceSender;
import ru.ntdev.srhr.common.audit.sender.teamcalendar.TeamCalendarPostEmployeeListSender;
import ru.ntdev.srhr.common.audit.sender.teamcalendar.TeamCalendarPostRequestResolutionSender;
import ru.ntdev.srhr.common.audit.sender.teamcalendar.TeamCalendarReportProcessingResultSender;
import ru.ntdev.srhr.common.audit.sender.teamcalendar.TeamCalendarSettingsSender;
import ru.ntdev.srhr.common.audit.sender.teamcalendar.TeamCalendarUserInfoSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetAbsenceEventSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetAssetSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetAttachmentContentSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetAttachmentListSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetAttachmentSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetAttachmentTypesSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetDeleteEventSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetGetEventSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetGetReportApproximateTimeSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetGetStaffListSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetGetStaffSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetLaunchSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetOrgunitSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetPostReportApproximateTimeSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetPostStaffSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetPresenceEventSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetPrintSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReferenceEventSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReferenceReportSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportApproverSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportFWTSSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportIWSDSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportIWSUSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportLogSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportParamsSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportPresenceSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportProcessingResultSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportTaskResultSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportTemplateSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetReportWRKTSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetRequestDetailsSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetRequestSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetScheduleSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetSubstitutionEventSender;
import ru.ntdev.srhr.common.audit.sender.timesheet.TimesheetTaskResultSender;
import ru.ntdev.srhr.common.audit.sender.userinfo.UserinfoChildSender;
import ru.ntdev.srhr.common.audit.sender.userinfo.UserinfoRolesSender;
import ru.ntdev.srhr.common.audit.sender.userinfo.UserinfoSender;
import ru.ntdev.srhr.common.audit.sender.userinfo.UserinfoTeamSender;
import ru.ntdev.srhr.common.audit.sender.userinfo.UserinfoUserlistSender;
import ru.ntdev.srhr.common.audit.sender.userphoto.UserphotoAvatarSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqAttachmentSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqAvdayCheckSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqAvdaySender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqCancelAvailableSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqCancelCancelSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqCancelDebtApplicationSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqCancelHistorySender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqDaysQuotaSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqDelegateSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqGetCancelSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqGetRecallSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqGetTransferSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqInfotextCancelSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqInfotextRecallSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqPostCancelSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqPostRecallSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqPostRequestSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqPostTransferSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqQuotaSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqRecallAvailableSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqRecallCancelSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqRecallDaysLeftAfterSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqRecallDebtApplicationSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqRecallHistorySender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqReqByIdSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqRequestSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqTransferAvailableSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqTransferCancelSender;
import ru.ntdev.srhr.common.audit.sender.vacationreq.VacationreqTransferHistorySender;
import ru.ntdev.srhr.common.audit.sender.yearbonus.YearbonusDetailsSender;
import ru.ntdev.srhr.common.audit.sender.yearbonus.YearbonusInfoSender;
import ru.ntdev.srhr.common.audit.sender.yearbonus.YearbonusReportSender;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Пиннинг-тест: фиксирует полный реестр аудит-кодов, перенесённых из
 * AuditEventSenderCustomImpl, и уникальность requestType.
 * Ожидаемые значения захардкожены намеренно — при изменении любого сендера
 * тест должен упасть.
 */
class AuditSenderRegistryTest {

    private record Expected(
            String requestType,
            AuditSender sender,
            String notFoundCode,
            String forbiddenCode,
            String errorCode
    ) {
    }

    private static final List<Expected> EXPECTED = List.of(
            new Expected("userinfo", new UserinfoSender(), "SRHR_USERINFO_STAFF_NOT_FOUND_GET_BY_ADLOGIN", "SRHR_USERINFO_REST_FORBIDDEN_GET_STAFF", "SRHR_USERINFO_REST_UNEXPECTED_ERROR"),
            new Expected("userinfo_team", new UserinfoTeamSender(), "SRHR_USERINFO_REST_NOT_FOUND_TEAM", "SRHR_USERINFO_REST_FORBIDDEN_TEAM", "SRHR_USERINFO_REST_UNEXPECTED_ERROR_TEAM"),
            new Expected("userinfo_userlist", new UserinfoUserlistSender(), "SRHR_USERINFO_REST_NOT_FOUND_USERLIST", "SRHR_USERINFO_REST_FORBIDDEN_USERLIST", "SRHR_USERINFO_REST_UNEXPECTED_ERROR_USERLIST"),
            new Expected("userinfo_child", new UserinfoChildSender(), "SRHR_USERINFO_REST_NOT_FOUND_CHILD", "SRHR_USERINFO_REST_FORBIDDEN_CHILD", "SRHR_USERINFO_REST_UNEXPECTED_ERROR_CHILD"),
            new Expected("userinfo_roles", new UserinfoRolesSender(), "SRHR_USERINFO_REST_NOT_FOUND_ROLES", "SRHR_USERINFO_REST_FORBIDDEN_ROLES", "SRHR_USERINFO_REST_UNEXPECTED_ERROR_ROLES"),
            new Expected("paystub_period", new PaystubPeriodSender(), "SRHR_PAYSTUB_REST_NOT_FOUND_PERIOD", "SRHR_PAYSTUB_REST_FORBIDDEN_PERIOD", "SRHR_PAYSTUB_REST_UNEXPECTED_ERROR_PERIOD"),
            new Expected("paystub_mail", new PaystubMailSender(), "SRHR_PAYSTUB_REST_NOT_FOUND_MAIL", "SRHR_PAYSTUB_REST_FORBIDDEN_MAIL", "SRHR_PAYSTUB_REST_UNEXPECTED_ERROR_MAIL"),
            new Expected("paystub_pdf", new PaystubPdfSender(), "SRHR_PAYSTUB_REST_NOT_FOUND_PDF", "SRHR_PAYSTUB_REST_FORBIDDEN_PDF", "SRHR_PAYSTUB_REST_UNEXPECTED_ERROR_PDF"),
            new Expected("paystub_body", new PaystubBodySender(), "SRHR_PAYSTUB_REST_NOT_FOUND_BODY", "SRHR_PAYSTUB_REST_FORBIDDEN_BODY", "SRHR_PAYSTUB_REST_UNEXPECTED_ERROR_BODY"),
            new Expected("paystub_head", new PaystubHeadSender(), "SRHR_PAYSTUB_REST_NOT_FOUND_HEAD", "SRHR_PAYSTUB_REST_FORBIDDEN_HEAD", "SRHR_PAYSTUB_REST_UNEXPECTED_ERROR_HEAD"),
            new Expected("payavg_event", new PayavgEventSender(), "SRHR_PAYAVG_REST_NOT_FOUND_EVENT", "SRHR_PAYAVG_REST_FORBIDDEN_EVENT", "SRHR_PAYAVG_REST_UNEXPECTED_ERROR_EVENT"),
            new Expected("payavg_calculate", new PayavgCalculateSender(), "SRHR_PAYAVG_REST_NOT_FOUND_CALCULATE", "SRHR_PAYAVG_REST_FORBIDDEN_CALCULATE", "SRHR_PAYAVG_REST_UNEXPECTED_ERROR_CALCULATE"),
            new Expected("vacationreq_request", new VacationreqRequestSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_REQUEST", "SRHR_VACATIONREQ_REST_FORBIDDEN_REQUEST", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_REQUEST"),
            new Expected("vacationreq_avday", new VacationreqAvdaySender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_AVDAY", "SRHR_VACATIONREQ_REST_FORBIDDEN_AVDAY", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_AVDAY"),
            new Expected("vacationreq_quota", new VacationreqQuotaSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_QUOTA", "SRHR_VACATIONREQ_REST_FORBIDDEN_QUOTA", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_QUOTA"),
            new Expected("vacationreq_req_by_id", new VacationreqReqByIdSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_REQBYID", "SRHR_VACATIONREQ_REST_FORBIDDEN_REQBYID", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_REQBYID"),
            new Expected("vacationreq_avday_check", new VacationreqAvdayCheckSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_AVDAY_CHECK", "SRHR_VACATIONREQ_REST_FORBIDDEN_AVDAY_CHECK", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_AVDAY_CHECK"),
            new Expected("vacationreq_days_quota", new VacationreqDaysQuotaSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_DAYS_QUOTA", "SRHR_VACATIONREQ_REST_FORBIDDEN_DAYS_QUOTA", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_DAYS_QUOTA"),
            new Expected("vacationreq_attachment", new VacationreqAttachmentSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_ATTACH", "SRHR_VACATIONREQ_REST_FORBIDDEN_ATTACH", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_ATTACH"),
            new Expected("vacationreq_post_request", new VacationreqPostRequestSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_POST_REQUEST", "SRHR_VACATIONREQ_REST_FORBIDDEN_POST_REQUEST", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_POST_REQUEST"),
            new Expected("vacationreq_delegate", new VacationreqDelegateSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_DELEGATE", "SRHR_VACATIONREQ_REST_FORBIDDEN_DELEGATE", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_DELEGATE"),
            new Expected("vacationreq_transfer_available", new VacationreqTransferAvailableSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_TRANSFER_AVAILABLE", "SRHR_VACATIONREQ_REST_FORBIDDEN_TRANSFER_AVAILABLE", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_TRANSFER_AVAILABLE"),
            new Expected("vacationreq_post_transfer", new VacationreqPostTransferSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_POST_TRANSFER", "SRHR_VACATIONREQ_REST_FORBIDDEN_POST_TRANSFER", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_POST_TRANSFER"),
            new Expected("vacationreq_transfer_history", new VacationreqTransferHistorySender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_TRANSFER_HISTORY", "SRHR_VACATIONREQ_REST_FORBIDDEN_TRANSFER_HISTORY", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_TRANSFER_HISTORY"),
            new Expected("vacationreq_get_transfer", new VacationreqGetTransferSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_GET_TRANSFER", "SRHR_VACATIONREQ_REST_FORBIDDEN_GET_TRANSFER", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_GET_TRANSFER"),
            new Expected("vacationreq_transfer_cancel", new VacationreqTransferCancelSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_TRANSFER_CANCEL", "SRHR_VACATIONREQ_REST_FORBIDDEN_TRANSFER_CANCEL", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_TRANSFER_CANCEL"),
            new Expected("vacationreq_recall_available", new VacationreqRecallAvailableSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_RECALL_AVAILABLE", "SRHR_VACATIONREQ_REST_FORBIDDEN_RECALL_AVAILABLE", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_RECALL_AVAILABLE"),
            new Expected("vacationreq_post_recall", new VacationreqPostRecallSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_POST_RECALL", "SRHR_VACATIONREQ_REST_FORBIDDEN_POST_RECALL", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_POST_RECALL"),
            new Expected("vacationreq_recall_history", new VacationreqRecallHistorySender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_RECALL_HISTORY", "SRHR_VACATIONREQ_REST_FORBIDDEN_RECALL_HISTORY", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_RECALL_HISTORY"),
            new Expected("vacationreq_get_recall", new VacationreqGetRecallSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_GET_RECALL", "SRHR_VACATIONREQ_REST_FORBIDDEN_GET_RECALL", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_GET_RECALL"),
            new Expected("vacationreq_recall_cancel", new VacationreqRecallCancelSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_RECALL_CANCEL", "SRHR_VACATIONREQ_REST_FORBIDDEN_RECALL_CANCEL", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_RECALL_CANCEL"),
            new Expected("vacationreq_cancel_available", new VacationreqCancelAvailableSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_CANCEL_AVAILABLE", "SRHR_VACATIONREQ_REST_FORBIDDEN_CANCEL_AVAILABLE", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_CANCEL_AVAILABLE"),
            new Expected("vacationreq_post_cancel", new VacationreqPostCancelSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_POST_CANCEL", "SRHR_VACATIONREQ_REST_FORBIDDEN_POST_CANCEL", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_POST_CANCEL"),
            new Expected("vacationreq_cancel_history", new VacationreqCancelHistorySender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_CANCEL_HISTORY", "SRHR_VACATIONREQ_REST_FORBIDDEN_CANCEL_HISTORY", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_CANCEL_HISTORY"),
            new Expected("vacationreq_get_cancel", new VacationreqGetCancelSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_GET_CANCEL", "SRHR_VACATIONREQ_REST_FORBIDDEN_GET_CANCEL", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_GET_CANCEL"),
            new Expected("vacationreq_cancel_cancel", new VacationreqCancelCancelSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_CANCEL_CANCEL", "SRHR_VACATIONREQ_REST_FORBIDDEN_CANCEL_CANCEL", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_CANCEL_CANCEL"),
            new Expected("vacationreq_infotext_cancel", new VacationreqInfotextCancelSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_INFOTEXT_CANCEL", "SRHR_VACATIONREQ_REST_FORBIDDEN_INFOTEXT_CANCEL", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_INFOTEXT_CANCEL"),
            new Expected("vacationreq_infotext_recall", new VacationreqInfotextRecallSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_INFOTEXT_RECALL", "SRHR_VACATIONREQ_REST_FORBIDDEN_INFOTEXT_RECALL", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_INFOTEXT_RECALL"),
            new Expected("vacationreq_cancel_debt_application", new VacationreqCancelDebtApplicationSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_CANCEL_DEBT_APPLICATION", "SRHR_VACATIONREQ_REST_FORBIDDEN_CANCEL_DEBT_APPLICATION", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_CANCEL_DEBT_APPLICATION"),
            new Expected("vacationreq_recall_debt_application", new VacationreqRecallDebtApplicationSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_RECALL_DEBT_APPLICATION", "SRHR_VACATIONREQ_REST_FORBIDDEN_RECALL_DEBT_APPLICATION", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_RECALL_DEBT_APPLICATION"),
            new Expected("vacationreq_recall_days_left_after", new VacationreqRecallDaysLeftAfterSender(), "SRHR_VACATIONREQ_REST_NOT_FOUND_RECALL_DAYS_LEFT_AFTER", "SRHR_VACATIONREQ_REST_FORBIDDEN_RECALL_DAYS_LEFT_AFTER", "SRHR_VACATIONREQ_REST_UNEXPECTED_ERROR_RECALL_DAYS_LEFT_AFTER"),
            new Expected("salarychange_staff_dependency", new SalarychangeStaffDependencySender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_STAFF_DEPENDENCY", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_STAFF_DEPENDENCY", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_STAFF_DEPENDENCY"),
            new Expected("salarychange_staff_dependency_exp", new SalarychangeStaffDependencyExpSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_STAFF_DEPENDENCY_EXP", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_STAFF_DEPENDENCY_EXP", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_STAFF_DEPENDENCY_EXP"),
            new Expected("salarychange_staff_detail", new SalarychangeStaffDetailSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_STAFF_DETAIL", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_STAFF_DETAIL", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_STAFF_DETAIL"),
            new Expected("salarychange_post_staff_detail", new SalarychangePostStaffDetailSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_POST_STAFF_DETAIL", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_POST_STAFF_DETAIL", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_POST_STAFF_DETAIL"),
            new Expected("salarychange_staff_delegation_list", new SalarychangeStaffDelegationListSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_STAFF_DELEGATION_LIST", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_STAFF_DELEGATION_LIST", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_STAFF_DELEGATION_LIST"),
            new Expected("salarychange_staff_list", new SalarychangeStaffListSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_STAFF_LIST", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_STAFF_LIST", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_STAFF_LIST"),
            new Expected("salarychange_staff_list_detail", new SalarychangeStaffListDetailSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_STAFF_LIST_DETAIL", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_STAFF_LIST_DETAIL", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_STAFF_LIST_DETAIL"),
            new Expected("salarychange_post_staff_list", new SalarychangePostStaffListSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_POST_STAFF_LIST", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_POST_STAFF_LIST", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_POST_STAFF_LIST"),
            new Expected("salarychange_post_staff_list_event", new SalarychangePostStaffListEventSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_POST_STAFF_LIST_EVENT", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_POST_STAFF_LIST_EVENT", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_POST_STAFF_LIST_EVENT"),
            new Expected("salarychange_reference_city", new SalarychangeReferenceCitySender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_REFERENCE_CITY", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_REFERENCE_CITY", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_REFERENCE_CITY"),
            new Expected("salarychange_reference_staff_position", new SalarychangeReferenceStaffPositionSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_REFERENCE_STAFF_POSITION", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_REFERENCE_STAFF_POSITION", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_REFERENCE_STAFF_POSITION"),
            new Expected("salarychange_reference_trm", new SalarychangeReferenceTrmSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_REFERENCE_TRM", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_REFERENCE_TRM", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_REFERENCE_TRM"),
            new Expected("salarychange_reference_delegation", new SalarychangeReferenceDelegationSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_REFERENCE_DELEGATION", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_REFERENCE_DELEGATION", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_REFERENCE_DELEGATION"),
            new Expected("salarychange_reference_ref", new SalarychangeReferenceRefSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_REFERENCE", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_REFERENCE", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_REFERENCE"),
            new Expected("salarychange_reference_tab", new SalarychangeReferenceTabSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_REFERENCE_TAB", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_REFERENCE_TAB", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_REFERENCE_TAB"),
            new Expected("salarychange_post_delegation", new SalarychangePostDelegationSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_POST_DELEGATION", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_POST_DELEGATION", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_POST_DELEGATION"),
            new Expected("salarychange_report_task_status", new SalarychangeReportTaskStatusSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_REPORT_TASK_STATUS", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_REPORT_TASK_STATUS", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_REPORT_TASK_STATUS"),
            new Expected("salarychange_post_report_task", new SalarychangePostReportTaskSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_POST_REPORT_TASK", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_POST_REPORT_TASK", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_POST_REPORT_TASK"),
            new Expected("salarychange_report_attachment", new SalarychangeReportAttachmentSender(), "SRHR_SALARYCHANGE_REST_NOT_FOUND_SALARYCHANGE_REPORT_ATTACHMENT", "SRHR_SALARYCHANGE_REST_FORBIDDEN_SALARYCHANGE_REPORT_ATTACHMENT", "SRHR_SALARYCHANGE_REST_UNEXPECTED_ERROR_SALARYCHANGE_REPORT_ATTACHMENT"),
            new Expected("schedule_holiday_user", new ScheduleHolidayUserSender(), "SRHR_SCHEDULE_HOLIDAY_REST_NOT_FOUND_SCHEDULE_HOLIDAY_USER", "SRHR_SCHEDULE_HOLIDAY_REST_FORBIDDEN_SCHEDULE_HOLIDAY_USER", "SRHR_SCHEDULE_HOLIDAY_REST_UNEXPECTED_ERROR_SCHEDULE_HOLIDAY_USER"),
            new Expected("schedule_holiday_ref", new ScheduleHolidayRefSender(), "SRHR_SCHEDULE_HOLIDAY_REST_NOT_FOUND_SCHEDULE_HOLIDAY_REF", "SRHR_SCHEDULE_HOLIDAY_REST_FORBIDDEN_SCHEDULE_HOLIDAY_REF", "SRHR_SCHEDULE_HOLIDAY_REST_UNEXPECTED_ERROR_SCHEDULE_HOLIDAY_REF"),
            new Expected("schedule_holiday_request", new ScheduleHolidayRequestSender(), "SRHR_SCHEDULE_HOLIDAY_REST_NOT_FOUND_SCHEDULE_HOLIDAY_REQUEST", "SRHR_SCHEDULE_HOLIDAY_REST_FORBIDDEN_SCHEDULE_HOLIDAY_REQUEST", "SRHR_SCHEDULE_HOLIDAY_REST_UNEXPECTED_ERROR_SCHEDULE_HOLIDAY_REQUEST"),
            new Expected("schedule_holiday_request_info", new ScheduleHolidayRequestInfoSender(), "SRHR_SCHEDULE_HOLIDAY_REST_NOT_FOUND_SCHEDULE_HOLIDAY_REQUEST_INFO", "SRHR_SCHEDULE_HOLIDAY_REST_FORBIDDEN_SCHEDULE_HOLIDAY_REQUEST_INFO", "SRHR_SCHEDULE_HOLIDAY_REST_UNEXPECTED_ERROR_SCHEDULE_HOLIDAY_REQUEST_INFO"),
            new Expected("schedule_holiday_post_request", new ScheduleHolidayPostRequestSender(), "SRHR_SCHEDULE_HOLIDAY_REST_NOT_FOUND_SCHEDULE_HOLIDAY_POST_REQUEST", "SRHR_SCHEDULE_HOLIDAY_REST_FORBIDDEN_SCHEDULE_HOLIDAY_POST_REQUEST", "SRHR_SCHEDULE_HOLIDAY_REST_UNEXPECTED_ERROR_SCHEDULE_HOLIDAY_POST_REQUEST"),
            new Expected("schedule_holiday_post_vacation_limit", new ScheduleHolidayPostVacationLimitSender(), "SRHR_SCHEDULE_HOLIDAY_REST_NOT_FOUND_SCHEDULE_HOLIDAY_POST_VACATION_LIMIT", "SRHR_SCHEDULE_HOLIDAY_REST_FORBIDDEN_SCHEDULE_HOLIDAY_POST_VACATION_LIMIT", "SRHR_SCHEDULE_HOLIDAY_REST_UNEXPECTED_ERROR_SCHEDULE_HOLIDAY_POST_VACATION_LIMIT"),
            new Expected("team_calendar_org_unit", new TeamCalendarOrgUnitSender(), "SRHR_TEAM_CALENDAR_REST_NOT_FOUND_TEAM_CALENDAR_ORG_UNIT", "SRHR_TEAM_CALENDAR_REST_FORBIDDEN_TEAM_CALENDAR_ORG_UNIT", "SRHR_TEAM_CALENDAR_REST_UNEXPECTED_ERROR_TEAM_CALENDAR_ORG_UNIT"),
            new Expected("team_calendar_page_reference", new TeamCalendarPageReferenceSender(), "SRHR_TEAM_CALENDAR_REST_NOT_FOUND_TEAM_CALENDAR_PAGE_REFERENCE", "SRHR_TEAM_CALENDAR_REST_FORBIDDEN_TEAM_CALENDAR_PAGE_REFERENCE", "SRHR_TEAM_CALENDAR_REST_UNEXPECTED_ERROR_TEAM_CALENDAR_PAGE_REFERENCE"),
            new Expected("team_calendar_user_info", new TeamCalendarUserInfoSender(), "SRHR_TEAM_CALENDAR_REST_NOT_FOUND_TEAM_CALENDAR_USER_INFO", "SRHR_TEAM_CALENDAR_REST_FORBIDDEN_TEAM_CALENDAR_USER_INFO", "SRHR_TEAM_CALENDAR_REST_UNEXPECTED_ERROR_TEAM_CALENDAR_USER_INFO"),
            new Expected("team_calendar_settings", new TeamCalendarSettingsSender(), "SRHR_TEAM_CALENDAR_REST_NOT_FOUND_TEAM_CALENDAR_SETTINGS", "SRHR_TEAM_CALENDAR_REST_FORBIDDEN_TEAM_CALENDAR_SETTINGS", "SRHR_TEAM_CALENDAR_REST_UNEXPECTED_ERROR_TEAM_CALENDAR_SETTINGS"),
            new Expected("team_calendar_org_unit_employee", new TeamCalendarOrgUnitEmployeeSender(), "SRHR_TEAM_CALENDAR_REST_NOT_FOUND_TEAM_CALENDAR_ORG_UNIT_EMPLOYEE", "SRHR_TEAM_CALENDAR_REST_FORBIDDEN_TEAM_CALENDAR_ORG_UNIT_EMPLOYEE", "SRHR_TEAM_CALENDAR_REST_UNEXPECTED_ERROR_TEAM_CALENDAR_ORG_UNIT_EMPLOYEE"),
            new Expected("team_calendar_post_employee_list", new TeamCalendarPostEmployeeListSender(), "SRHR_TEAM_CALENDAR_REST_NOT_FOUND_TEAM_CALENDAR_POST_EMPLOYEE_LIST", "SRHR_TEAM_CALENDAR_REST_FORBIDDEN_TEAM_CALENDAR_POST_EMPLOYEE_LIST", "SRHR_TEAM_CALENDAR_REST_UNEXPECTED_ERROR_TEAM_CALENDAR_POST_EMPLOYEE_LIST"),
            new Expected("team_calendar_post_request_resolution", new TeamCalendarPostRequestResolutionSender(), "SRHR_TEAM_CALENDAR_REST_NOT_FOUND_TEAM_CALENDAR_POST_REQUEST_RESOLUTION", "SRHR_TEAM_CALENDAR_REST_FORBIDDEN_TEAM_CALENDAR_POST_REQUEST_RESOLUTION", "SRHR_TEAM_CALENDAR_REST_UNEXPECTED_ERROR_TEAM_CALENDAR_POST_REQUEST_RESOLUTION"),
            new Expected("team_calendar_get_request_resolution", new TeamCalendarGetRequestResolutionSender(), "SRHR_TEAM_CALENDAR_REST_NOT_FOUND_TEAM_CALENDAR_GET_REQUEST_RESOLUTION", "SRHR_TEAM_CALENDAR_REST_FORBIDDEN_TEAM_CALENDAR_GET_REQUEST_RESOLUTION", "SRHR_TEAM_CALENDAR_REST_UNEXPECTED_ERROR_TEAM_CALENDAR_GET_REQUEST_RESOLUTION"),
            new Expected("team_calendar_report_processing_result", new TeamCalendarReportProcessingResultSender(), "SRHR_TEAM_CALENDAR_REST_NOT_FOUND_TEAM_CALENDAR_REPORT_PROCESSING_RESULT", "SRHR_TEAM_CALENDAR_REST_FORBIDDEN_TEAM_CALENDAR_REPORT_PROCESSING_RESULT", "SRHR_TEAM_CALENDAR_REST_UNEXPECTED_ERROR_TEAM_CALENDAR_REPORT_PROCESSING_RESULT"),
            new Expected("timesheet_reference_event", new TimesheetReferenceEventSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REFERENCE_EVENT", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REFERENCE_EVENT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REFERENCE_EVENT"),
            new Expected("timesheet_reference_report", new TimesheetReferenceReportSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REFERENCE_REPORT", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REFERENCE_REPORT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REFERENCE_REPORT"),
            new Expected("timesheet_orgunit", new TimesheetOrgunitSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_ORGUNIT", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_ORGUNIT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_ORGUNIT"),
            new Expected("timesheet_get_staff_list", new TimesheetGetStaffListSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_GET_STAFF_LIST", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_GET_STAFF_LIST", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_GET_STAFF_LIST"),
            new Expected("timesheet_get_staff", new TimesheetGetStaffSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_GET_STAFF", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_GET_STAFF", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_GET_STAFF"),
            new Expected("timesheet_post_staff", new TimesheetPostStaffSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_POST_STAFF", "SRHR_TIMESHEET_REST_FORBIDDEN_POST_STAFF", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_POST_STAFF"),
            new Expected("timesheet_schedule", new TimesheetScheduleSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_SCHEDULE", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_SCHEDULE", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_SCHEDULE"),
            new Expected("timesheet_task_result", new TimesheetTaskResultSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_TASK_RESULT", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_TASK_RESULT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_TASK_RESULT"),
            new Expected("timesheet_report_task_result", new TimesheetReportTaskResultSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REPORT_TASK_RESULT", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REPORT_TASK_RESULT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REPORT_TASK_RESULT"),
            new Expected("timesheet_attachment", new TimesheetAttachmentSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_ATTACHMENT", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_ATTACHMENT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_ATTACHMENT"),
            new Expected("timesheet_get_event", new TimesheetGetEventSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_EVENT", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_EVENT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_EVENT"),
            new Expected("timesheet_delete_event", new TimesheetDeleteEventSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_DELETE_TIMESHEET_EVENT", "SRHR_TIMESHEET_REST_FORBIDDEN_DELETE_TIMESHEET_EVENT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_DELETE_TIMESHEET_EVENT"),
            new Expected("timesheet_presence_event", new TimesheetPresenceEventSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_EVENT_PRESENCE", "SRHR_TIMESHEET_REST_FORBIDDEN_EVENT_PRESENCE", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_EVENT_PRESENCE"),
            new Expected("timesheet_absence_event", new TimesheetAbsenceEventSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_EVENT_ABSENCE", "SRHR_TIMESHEET_REST_FORBIDDEN_EVENT_ABSENCE", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_EVENT_ABSENCE"),
            new Expected("timesheet_substitution_event", new TimesheetSubstitutionEventSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_EVENT_SUBSTITUTION", "SRHR_TIMESHEET_REST_FORBIDDEN_EVENT_SUBSTITUTION", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_EVENT_SUBSTITUTION"),
            new Expected("timesheet_print", new TimesheetPrintSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_FORM", "SRHR_TIMESHEET_REST_FORBIDDEN_FORM", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_FORM"),
            new Expected("timesheet_launch", new TimesheetLaunchSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_SIGNED_AGREEMENT", "SRHR_TIMESHEET_REST_FORBIDDEN_SIGNED_AGREEMENT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_SIGNED_AGREEMENT"),
            new Expected("timesheet_report_params", new TimesheetReportParamsSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REPORT_PARAMS", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REPORT_PARAMS", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REPORT_PARAMS"),
            new Expected("timesheet_report_template", new TimesheetReportTemplateSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REPORT_TEMPLATE", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REPORT_TEMPLATE", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REPORT_TEMPLATE"),
            new Expected("timesheet_report_log", new TimesheetReportLogSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REPORT_LOG", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REPORT_LOG", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REPORT_LOG"),
            new Expected("timesheet_report_processing_result", new TimesheetReportProcessingResultSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REPORT_PROCESSING_RESULT", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REPORT_PROCESSING_RESULT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REPORT_PROCESSING_RESULT"),
            new Expected("timesheet_report_presence", new TimesheetReportPresenceSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_REPORT_PRESENCE", "SRHR_TIMESHEET_REST_FORBIDDEN_REPORT_PRESENCE", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_REPORT_PRESENCE"),
            new Expected("timesheet_report_iwsu", new TimesheetReportIWSUSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_REPORT_IWSU", "SRHR_TIMESHEET_REST_FORBIDDEN_REPORT_IWSU", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_REPORT_IWSU"),
            new Expected("timesheet_report_iwsd", new TimesheetReportIWSDSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_REPORT_IWSD", "SRHR_TIMESHEET_REST_FORBIDDEN_REPORT_IWSD", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_REPORT_IWSD"),
            new Expected("timesheet_report_wrkt", new TimesheetReportWRKTSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_REPORT_WRKT", "SRHR_TIMESHEET_REST_FORBIDDEN_REPORT_WRKT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_REPORT_WRKT"),
            new Expected("timesheet_report_fwts", new TimesheetReportFWTSSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_REPORT_FWTS", "SRHR_TIMESHEET_REST_FORBIDDEN_REPORT_FWTS", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_REPORT_FWTS"),
            new Expected("timesheet_attachment_types", new TimesheetAttachmentTypesSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_ATTACH_TYPE", "SRHR_TIMESHEET_REST_FORBIDDEN_ATTACH_TYPE", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_ATTACH_TYPE"),
            new Expected("timesheet_report_approver", new TimesheetReportApproverSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_REPORT_APPROVER", "SRHR_TIMESHEET_REST_FORBIDDEN_REPORT_APPROVER", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_REPORT_APPROVER"),
            new Expected("timesheet_asset", new TimesheetAssetSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_ASSET", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_ASSET", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_ASSET"),
            new Expected("timesheet_request", new TimesheetRequestSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REQUEST", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REQUEST", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REQUEST"),
            new Expected("timesheet_request_details", new TimesheetRequestDetailsSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REQUEST_DETAILS", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REQUEST_DETAILS", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REQUEST_DETAILS"),
            new Expected("timesheet_attachment_content", new TimesheetAttachmentContentSender(), "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_ATTACHMENT_CONTENT", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_ATTACHMENT_CONTENT", "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_ATTACHMENT_CONTENT"),
            new Expected("timesheet_attachment_list", new TimesheetAttachmentListSender(), "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_ATTACHMENT_LIST", "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_ATTACHMENT_LIST", "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_ATTACHMENT_LIST"),
            new Expected("timesheet_get_report_approximate_time", new TimesheetGetReportApproximateTimeSender(), "SRHR_TIMESHEET_REST_GET_NOT_FOUND_REPORT_APPROXIMATE_TIME", "SRHR_TIMESHEET_REST_GET_FORBIDDEN_REPORT_APPROXIMATE_TIME", "SRHR_TIMESHEET_REST_GET_UNEXPECTED_ERROR_REPORT_APPROXIMATE_TIME"),
            new Expected("timesheet_post_report_approximate_time", new TimesheetPostReportApproximateTimeSender(), "SRHR_TIMESHEET_REST_POST_NOT_FOUND_REPORT_APPROXIMATE_TIME", "SRHR_TIMESHEET_REST_POST_FORBIDDEN_REPORT_APPROXIMATE_TIME", "SRHR_TIMESHEET_REST_POST_UNEXPECTED_ERROR_REPORT_APPROXIMATE_TIME"),
            new Expected("userphoto_avatar", new UserphotoAvatarSender(), "SRHR_USERPHOTO_REST_NOT_FOUND_AVATAR", "SRHR_USERPHOTO_REST_FORBIDDEN_AVATAR", "SRHR_USERPHOTO_REST_UNEXPECTED_ERROR_AVATAR"),
            new Expected("yearbonus_info", new YearbonusInfoSender(), "SRHR_YEARBONUS_REST_NOT_FOUND_INFO", "SRHR_YEARBONUS_REST_FORBIDDEN_INFO", "SRHR_YEARBONUS_REST_UNEXPECTED_ERROR_INFO"),
            new Expected("yearbonus_details", new YearbonusDetailsSender(), "SRHR_YEARBONUS_REST_NOT_FOUND_DETAILS", "SRHR_YEARBONUS_REST_FORBIDDEN_DETAILS", "SRHR_YEARBONUS_REST_UNEXPECTED_ERROR_DETAILS"),
            new Expected("yearbonus_report", new YearbonusReportSender(), "SRHR_YEARBONUS_REST_NOT_FOUND_REPORT", "SRHR_YEARBONUS_REST_FORBIDDEN_REPORT", "SRHR_YEARBONUS_REST_UNEXPECTED_ERROR_REPORT"),
            new Expected("requisition_delegations", new RequisitionDelegationsSender(), "SRHR_REQUISITION_NOT_FOUND_GET_DELEGATIONS", "SRHR_REQUISITION_FORBIDDEN_GET_DELEGATIONS", "SRHR_REQUISITION_UNEXPECTED_ERROR_GET_DELEGATIONS"),
            new Expected("requisition_dict", new RequisitionDictSender(), "SRHR_REQUISITION_NOT_FOUND_GET_DICT", "SRHR_REQUISITION_FORBIDDEN_GET_DICT", "SRHR_REQUISITION_UNEXPECTED_ERROR_GET_DICT"),
            new Expected("requisition_org_units_allowable", new RequisitionOrgUnitsAllowableSender(), "SRHR_REQUISITION_NOT_FOUND_GET_ORG_UNITS_ALLOWABLE", "SRHR_REQUISITION_FORBIDDEN_GET_ORG_UNITS_ALLOWABLE", "SRHR_REQUISITION_UNEXPECTED_ERROR_GET_ORG_UNITS_ALLOWABLE"),
            new Expected("requisition_post_list", new RequisitionPostListSender(), "SRHR_REQUISITION_NOT_FOUND_POST_LIST", "SRHR_REQUISITION_FORBIDDEN_POST_LIST", "SRHR_REQUISITION_UNEXPECTED_ERROR_POST_LIST"),
            new Expected("requisition_ui_state", new RequisitionUiStateSender(), "SRHR_REQUISITION_NOT_FOUND_GET_UI_STATE", "SRHR_REQUISITION_FORBIDDEN_GET_UI_STATE", "SRHR_REQUISITION_UNEXPECTED_ERROR_GET_UI_STATE"),
            new Expected("requisition_user_info", new RequisitionUserInfoSender(), "SRHR_REQUISITION_NOT_FOUND_GET_USER_INFO", "SRHR_REQUISITION_FORBIDDEN_GET_USER_INFO", "SRHR_REQUISITION_UNEXPECTED_ERROR_GET_USER_INFO"),
            new Expected("requisition_staff_search", new RequisitionStaffSearchSender(), "SRHR_REQUISITION_GET_STAFF_SEARCH_NOT_FOUND", "SRHR_REQUISITION_GET_STAFF_SEARCH_FORBIDDEN", "SRHR_REQUISITION_GET_STAFF_SEARCH_UNEXPECTED_ERROR"),
            new Expected("requisition_vacancy_candidates", new RequisitionVacancyCandidatesSender(), "SRHR_REQUISITION_POST_VACANCY_CANDIDATES_NOT_FOUND", "SRHR_REQUISITION_POST_VACANCY_CANDIDATES_FORBIDDEN", "SRHR_REQUISITION_POST_VACANCY_CANDIDATES_UNEXPECTED_ERROR"),
            new Expected("requisition_vacancy_details", new RequisitionVacancyDetailsSender(), "SRHR_REQUISITION_GET_VACANCY_DETAILS_NOT_FOUND", "SRHR_REQUISITION_GET_VACANCY_DETAILS_FORBIDDEN", "SRHR_REQUISITION_GET_VACANCY_DETAILS_UNEXPECTED_ERROR"),
            new Expected("requisition_candidate_action", new RequisitionCandidateActionSender(), "SRHR_REQUISITION_POST_CANDIDATE_ACTION_NOT_FOUND", "SRHR_REQUISITION_POST_CANDIDATE_ACTION_FORBIDDEN", "SRHR_REQUISITION_POST_CANDIDATE_ACTION_UNEXPECTED_ERROR"),
            new Expected("requisition_get_interviewers", new RequisitionGetInterviewersSender(), "SRHR_REQUISITION_GET_GET_INTERVIEWERS_NOT_FOUND", "SRHR_REQUISITION_GET_GET_INTERVIEWERS_FORBIDDEN", "SRHR_REQUISITION_GET_GET_INTERVIEWERS_UNEXPECTED_ERROR"),
            new Expected("requisition_interviewers_action", new RequisitionInterviewersActionSender(), "SRHR_REQUISITION_POST_INTERVIEWERS_ACTION_NOT_FOUND", "SRHR_REQUISITION_POST_INTERVIEWERS_ACTION_FORBIDDEN", "SRHR_REQUISITION_POST_INTERVIEWERS_ACTION_UNEXPECTED_ERROR"),
            new Expected("requisition_candidate_info", new RequisitionCandidateInfoSender(), "SRHR_REQUISITION_GET_CANDIDATE_INFO_NOT_FOUND", "SRHR_REQUISITION_GET_CANDIDATE_INFO_FORBIDDEN", "SRHR_REQUISITION_GET_CANDIDATE_INFO_UNEXPECTED_ERROR"),
            new Expected("requisition_vacancy_statistics", new RequisitionVacancyStatisticsSender(), "SRHR_REQUISITION_GET_VACANCY_STATISTICS_NOT_FOUND", "SRHR_REQUISITION_GET_VACANCY_STATISTICS_FORBIDDEN", "SRHR_REQUISITION_GET_VACANCY_STATISTICS_UNEXPECTED_ERROR"),
            new Expected("requisition_candidate_resume", new RequisitionCandidateResumeSender(), "SRHR_REQUISITION_GET_CANDIDATE_RESUME_NOT_FOUND", "SRHR_REQUISITION_GET_CANDIDATE_RESUME_FORBIDDEN", "SRHR_REQUISITION_GET_CANDIDATE_RESUME_UNEXPECTED_ERROR"),
            new Expected("requisition_add_comment", new RequisitionAddCommentSender(), "SRHR_REQUISITION_POST_ADD_COMMENT_NOT_FOUND", "SRHR_REQUISITION_POST_ADD_COMMENT_FORBIDDEN", "SRHR_REQUISITION_POST_ADD_COMMENT_UNEXPECTED_ERROR"),
            new Expected("requisition_candidate_recommendation", new RequisitionCandidateRecommendationSender(), "SRHR_REQUISITION_POST_CANDIDATE_RECOMMENDATION_NOT_FOUND", "SRHR_REQUISITION_POST_CANDIDATE_RECOMMENDATION_FORBIDDEN", "SRHR_REQUISITION_POST_CANDIDATE_RECOMMENDATION_UNEXPECTED_ERROR"),
            new Expected("requisition_vacancy_info", new RequisitionVacancyInfoSender(), "SRHR_REQUISITION_POST_VACANCY_INFO_NOT_FOUND", "SRHR_REQUISITION_POST_VACANCY_INFO_FORBIDDEN", "SRHR_REQUISITION_POST_VACANCY_INFO_UNEXPECTED_ERROR"),
            new Expected("collect_pending_candidates", new CollectPendingCandidatesSender(), "SRHR_REQUISITION_PENDING_CANDIDATES_NOT_FOUND", "SRHR_REQUISITION_PENDING_CANDIDATES_FORBIDDEN", "SRHR_REQUISITION_PENDING_CANDIDATES_UNEXPECTED_ERROR")
    );

    @Test
    void allSendersPresent() {
        assertThat(EXPECTED).hasSize(133);
    }

    @Test
    void requestTypesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (Expected expected : EXPECTED) {
            assertThat(seen.add(expected.requestType()))
                    .as("Дубликат requestType: %s", expected.requestType())
                    .isTrue();
        }
    }

    @Test
    void eachRequestTypeSupportedByExactlyOneSender() {
        for (Expected target : EXPECTED) {
            long count = EXPECTED.stream()
                    .filter(e -> e.sender().supports(target.requestType()))
                    .count();
            assertThat(count)
                    .as("requestType '%s' должен поддерживаться ровно одним сендером", target.requestType())
                    .isEqualTo(1);
        }
    }

    @Test
    void auditCodesMatchLegacyRegistry() {
        for (Expected expected : EXPECTED) {
            AuditSender sender = expected.sender();
            assertThat(sender.supports(expected.requestType()))
                    .as("%s должен поддерживать '%s'", sender.getClass().getSimpleName(), expected.requestType())
                    .isTrue();
            assertThat(sender.eventNotFoundCode())
                    .as("%s: notFound", sender.getClass().getSimpleName())
                    .isEqualTo(expected.notFoundCode());
            assertThat(sender.eventForbiddenCode())
                    .as("%s: forbidden", sender.getClass().getSimpleName())
                    .isEqualTo(expected.forbiddenCode());
            assertThat(sender.eventErrorCode())
                    .as("%s: error", sender.getClass().getSimpleName())
                    .isEqualTo(expected.errorCode());
        }
    }
}
