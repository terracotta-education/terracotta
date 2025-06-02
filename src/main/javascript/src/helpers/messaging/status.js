var status = {
  attachment: {
    created: "CREATED",
    deleted: "DELETED",
    error: "ERROR",
    uploaded: "UPLOADED"
  },
  message: {
    canceled: "CANCELED",
    copied: "COPIED",
    created: "CREATED",
    deleted: "DELETED",
    disabled: "DISABLED",
    edited: "EDITED",
    error: "ERROR",
    incomplete: "INCOMPLETE",
    processing: "PROCESSING",
    published: "PUBLISHED",
    queued: "QUEUED",
    ready: "READY",
    sent: "SENT",
    unpublished: "UNPUBLISHED"
  }
};

export var attachment = status.attachment;
export var message = status.message;

export var editableMessageStatuses = [
  message.copied,
  message.created,
  message.disabled,
  message.edited,
  message.incomplete,
  message.published,
  message.ready,
  message.unpublished
];

export var incompleteMessageStatuses = [
  message.copied,
  message.created,
  message.edited,
  message.canceled,
  message.deleted,
  message.error,
  message.incomplete
]
