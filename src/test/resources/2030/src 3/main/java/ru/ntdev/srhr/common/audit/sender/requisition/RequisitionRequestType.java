package ru.ntdev.srhr.common.audit.sender.requisition;

public enum RequisitionRequestType {

    REQUISITION_DELEGATIONS("requisition_delegations"),
    REQUISITION_DICT("requisition_dict"),
    REQUISITION_ORG_UNITS_ALLOWABLE("requisition_org_units_allowable"),
    REQUISITION_POST_LIST("requisition_post_list"),
    REQUISITION_UI_STATE("requisition_ui_state"),
    REQUISITION_USER_INFO("requisition_user_info"),
    REQUISITION_STAFF_SEARCH("requisition_staff_search"),
    REQUISITION_VACANCY_CANDIDATES("requisition_vacancy_candidates"),
    REQUISITION_VACANCY_DETAILS("requisition_vacancy_details"),
    REQUISITION_CANDIDATE_ACTION("requisition_candidate_action"),
    REQUISITION_GET_INTERVIEWERS("requisition_get_interviewers"),
    REQUISITION_INTERVIEWERS_ACTION("requisition_interviewers_action"),
    REQUISITION_CANDIDATE_INFO("requisition_candidate_info"),
    REQUISITION_VACANCY_STATISTICS("requisition_vacancy_statistics"),
    REQUISITION_CANDIDATE_RESUME("requisition_candidate_resume"),
    REQUISITION_ADD_COMMENT("requisition_add_comment"),
    REQUISITION_CANDIDATE_RECOMMENDATION("requisition_candidate_recommendation"),
    REQUISITION_VACANCY_INFO("requisition_vacancy_info"),
    COLLECT_PENDING_CANDIDATES("collect_pending_candidates");

    private final String value;

    RequisitionRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
