/**
 * Public explanations for implementation statuses. These descriptions are
 * deliberately cautious: an approved document is not proof of completion and
 * a lack of assessment is not evidence of inaction.
 */
export const STATUS_DESCRIPTIONS = {
    NOT_ASSESSED: "Not enough verified information to assess progress yet. This is not evidence that nothing has happened.",
    NOT_STARTED: "No implementation has been established by the verified evidence.",
    IN_PROGRESS: "Implementation has begun and is supported by verified evidence.",
    DELAYED: "Implementation is behind its documented schedule.",
    COMPLETED: "Completion has been independently verified through the status review workflow.",
    PARTIALLY_COMPLETED: "Part of the commitment has been independently verified as complete.",
    CANCELLED: "The commitment was formally withdrawn or cancelled.",
    DISPUTED: "Available evidence or assessments conflict; the status is under dispute."
};

export const STATUS_LABELS = {
    NOT_ASSESSED: "Not yet assessed",
    NOT_STARTED: "Not started",
    IN_PROGRESS: "In progress",
    COMPLETED: "Completed",
    DELAYED: "Delayed",
    PARTIALLY_COMPLETED: "Partially completed",
    CANCELLED: "Cancelled",
    DISPUTED: "Disputed"
};

export function statusLabel(status) {
    return STATUS_LABELS[status] || status || "Not yet assessed";
}

export function statusDescription(status) {
    return STATUS_DESCRIPTIONS[status] ||
        "No verified assessment is available for this status.";
}

export function formatDate(value) {
    if (!value) return "";
    return new Date(value).toLocaleDateString();
}

export function formatDateTime(value) {
    if (!value) return "";
    return new Date(value).toLocaleString();
}
