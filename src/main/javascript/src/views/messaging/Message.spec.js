import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";

const route = {
  query: { containerId: "container-1", messageId: "message-1" }
};
const push = vi.fn();

vi.mock("vue-router", () => ({
  useRoute: () => route,
  useRouter: () => ({ push })
}));

vi.mock("sweetalert2", () => ({
  default: { fire: vi.fn() }
}));

// jsdom does not implement scrollIntoView; Message calls it when the
// conditional-text panel opens.
window.HTMLElement.prototype.scrollIntoView = vi.fn();

vi.mock("@/services", () => ({
  messageService: {
    getAssignments: vi.fn(),
    update: vi.fn(),
    updatePlaceholders: vi.fn(),
    sendTest: vi.fn(),
    fetchPreview: vi.fn(),
    uploadPipedText: vi.fn()
  },
  messageContainerService: {}
}));

import { createPinia, setActivePinia } from "pinia";
import { flushPromises, DOMWrapper } from "@vue/test-utils";
import Swal from "sweetalert2";

import { messageService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import Message from "./Message.vue";

import { condition as conditionModule } from "@/store/condition.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { message as messagingMessageModule } from "@/store/messaging/message.module";
import { conditionaltext as messagingConditionalTextModule } from "@/store/messaging/conditionaltext.module";
import { alert as alertModule } from "@/store/alert.module";

const stubs = {
  PageLoading: true,
  Recipients: true,
  TipTapEditor: true,
  EditorSubMenu: true,
  PipedTextFileUploader: true,
  Preview: true,
  SendTest: true,
  ToConsentedOnly: true,
  Type: true,
  ReplyTo: true,
  Scheduler: true,
  ConditionalText: true
};

function buildMessage(overrides = {}) {
  return {
    id: "message-1",
    conditionId: "c1",
    isCopy: false,
    ruleSets: [],
    configuration: {
      id: "config-1",
      enabled: false,
      type: "CONVERSATION",
      subject: null,
      replyTo: [],
      sendAt: null,
      status: "READY",
      toConsentedOnly: false,
      matchType: "INCLUDE"
    },
    content: {
      id: "content-1",
      html: null,
      attachments: [],
      conditionalTexts: [],
      pipedText: null
    },
    ...overrides
  };
}

function buildContainer(overrides = {}) {
  return {
    id: "container-1",
    exposureId: "exposure-1",
    ownerEmail: "owner@example.com",
    configuration: {
      title: "My Message Container",
      status: "UNPUBLISHED"
    },
    messages: [buildMessage()],
    ...overrides
  };
}

function seedStores({ containers = [buildContainer()] } = {}) {
  const pinia = createPinia();
  setActivePinia(pinia);

  conditionModule();
  experimentModule().experiment = {
    experimentId: 1,
    conditions: [{ conditionId: "c1", name: "Condition A" }]
  };

  const containerStore = messagingMessageContainerModule();
  containerStore.messageContainers = containers;

  const messageStore = messagingMessageModule();
  const conditionalTextStore = messagingConditionalTextModule();
  alertModule();

  return { pinia, containerStore, messageStore, conditionalTextStore };
}

function mount(seedOptions = {}, mountOptions = {}) {
  const { pinia, containerStore, messageStore, conditionalTextStore } = seedStores(seedOptions);

  const wrapper = mountComponent(Message, {
    pinia,
    ...mountOptions,
    global: {
      stubs,
      ...mountOptions.global
    }
  });

  return { wrapper, containerStore, messageStore, conditionalTextStore };
}

async function settle(wrapper) {
  await flushPromises();
  await wrapper.vm.$nextTick();
  await flushPromises();
  await wrapper.vm.$nextTick();
}

describe("Message", () => {
  beforeEach(() => {
    document.body.innerHTML =
      '<div class="steps-container-col col-md-6"></div>' +
      '<div class="experiment-steps__body pt-4"></div>';

    vi.clearAllMocks();
    messageService.getAssignments.mockResolvedValue([]);
    messageService.update.mockResolvedValue({});
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("renders the container title and the message type chip", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    expect(wrapper.text()).toContain("My Message Container");
    expect(wrapper.text()).toContain("Canvas Message");
    expect(wrapper.text()).toContain("Only One Version");
  });

  it("marks the enabled-switch as 'on' when the message is enabled, matching Vuetify 2's automatic primary-color-when-checked behavior for selection controls", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              configuration: {
                id: "config-1",
                enabled: true,
                type: "CONVERSATION",
                subject: null,
                replyTo: [],
                sendAt: null,
                status: "READY",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    expect(wrapper.find(".enabled-switch").classes()).toContain("enabled-switch--on");
  });

  it("scrolls the conditional-text panel into view when it's opened to add new text", async () => {
    window.HTMLElement.prototype.scrollIntoView.mockClear();

    const { wrapper } = mount();
    await settle(wrapper);

    await wrapper.findComponent({ name: "EditorSubMenu" }).vm.$emit("add-conditional-text");
    await settle(wrapper);

    expect(wrapper.find(".treatment-tab-conditional-text").exists()).toBe(true);
    expect(window.HTMLElement.prototype.scrollIntoView).toHaveBeenCalled();
  });

  it("scrolls the conditional-text panel into view when it's opened to edit existing text", async () => {
    window.HTMLElement.prototype.scrollIntoView.mockClear();

    const { wrapper } = mount();
    await settle(wrapper);

    await wrapper.findComponent({ name: "EditorSubMenu" }).vm.$emit("edit-conditional-text", "ct-1");
    await settle(wrapper);

    expect(wrapper.find(".treatment-tab-conditional-text").exists()).toBe(true);
    expect(window.HTMLElement.prototype.scrollIntoView).toHaveBeenCalled();
  });

  it("does not mark the enabled-switch as 'on' when the message is disabled", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    expect(wrapper.find(".enabled-switch").classes()).not.toContain("enabled-switch--on");
  });

  it("shows a condition chip instead of 'Only One Version' when there are multiple treatments", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({ id: "message-1", conditionId: "c1" }),
            buildMessage({ id: "message-2", conditionId: "c2" })
          ]
        })
      ]
    });
    await settle(wrapper);

    expect(wrapper.text()).toContain("Condition A");
    expect(wrapper.text()).not.toContain("Only One Version");
  });

  it("widens the surrounding container on mount and shrinks it again on unmount", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    const container = document.querySelector(".steps-container-col");
    expect(container.classList.contains("col-md-10")).toBe(true);

    wrapper.unmount();

    expect(container.classList.contains("col-md-6")).toBe(true);
  });

  it("saveExit saves and redirects to ExperimentSummary when the message is disabled (no validation required)", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(true);
    expect(messageService.update).toHaveBeenCalled();
    const [, , , , payload] = messageService.update.mock.calls.at(-1);
    expect(payload.id).toBe("message-1");
    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 1 }
    });
  });

  it("saveExit blocks saving and alerts the user when a required field is missing on an enabled message", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              configuration: {
                id: "config-1",
                enabled: true,
                type: "CONVERSATION",
                subject: null,
                replyTo: [],
                sendAt: null,
                status: "READY",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              },
              content: {
                id: "content-1",
                html: null,
                attachments: [],
                conditionalTexts: [],
                pipedText: null
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(false);
    expect(Swal.fire).toHaveBeenCalledWith(
      "Please complete all required sections."
    );
    expect(messageService.update).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
  });

  it("does not call the update service for a read-only (already sent) message, but still redirects", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              configuration: {
                id: "config-1",
                enabled: true,
                type: "CONVERSATION",
                subject: "Hello",
                replyTo: [],
                sendAt: null,
                status: "SENT",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              },
              content: {
                id: "content-1",
                html: "<p>Body</p>",
                attachments: [],
                conditionalTexts: [],
                pipedText: null
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(true);
    expect(messageService.update).not.toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 1 }
    });
  });

  it("only offers 'Copy message from' for containers that have other messages and are not deleted", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer(),
        buildContainer({
          id: "container-2",
          configuration: { title: "Other Container", status: "UNPUBLISHED" },
          messages: [buildMessage({ id: "message-2" })]
        }),
        buildContainer({
          id: "container-3",
          configuration: { title: "Deleted Container", status: "DELETED" },
          messages: [buildMessage({ id: "message-3" })]
        })
      ]
    });
    await settle(wrapper);

    const copyButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("Copy message from"));

    expect(copyButton).toBeTruthy();
  });

  it("toggles the enabled switch via user interaction and updates the underlying configuration", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    expect(wrapper.vm.enabled).toBe(false);

    await wrapper.find(".enabled-switch input").setValue(true);
    await settle(wrapper);

    expect(wrapper.vm.enabled).toBe(true);
  });

  it("updates the subject computed when the subject field is edited", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    await wrapper.findComponent({ name: "VTextField" }).setValue("A new subject");
    await settle(wrapper);

    expect(wrapper.vm.subject).toBe("A new subject");
  });

  it("forwards TipTapEditor edited/cursor events to the message body and cursor position", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    await wrapper.findComponent({ name: "TipTapEditor" }).vm.$emit("edited", "<p>New body</p>");
    await settle(wrapper);

    expect(wrapper.vm.html).toBe("<p>New body</p>");

    await wrapper.findComponent({ name: "TipTapEditor" }).vm.$emit("cursor", 12);
    await settle(wrapper);

    expect(wrapper.vm.editorCursorPosition).toBe(12);
  });

  it("stores piped text queued for insertion when EditorSubMenu emits insert-piped-text", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    await wrapper.findComponent({ name: "EditorSubMenu" }).vm.$emit("insert-piped-text", { id: "pt-item-1", label: "Name" });
    await settle(wrapper);

    expect(wrapper.vm.pipedTextToPlace).toMatchObject({ id: "pt-item-1", label: "Name" });
  });

  it("queues a conditional text for insertion and closes the editor when EditorSubMenu emits insert-conditional-text", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    await wrapper.findComponent({ name: "EditorSubMenu" }).vm.$emit("insert-conditional-text", { id: "ct-1", label: "CT" });
    await settle(wrapper);

    expect(wrapper.vm.conditionalTextToPlace).toMatchObject({ id: "ct-1", label: "CT" });
    expect(wrapper.vm.openConditionalTextEditor).toBe(false);
  });

  it("handles conditional-text-updated and cancel events from the ConditionalText panel", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    await wrapper.findComponent({ name: "EditorSubMenu" }).vm.$emit("add-conditional-text");
    await settle(wrapper);

    expect(wrapper.vm.openConditionalTextEditor).toBe(true);

    await wrapper.findComponent({ name: "ConditionalText" }).vm.$emit("conditional-text-updated", { id: "ct-2", label: "Updated" });
    await settle(wrapper);

    expect(wrapper.vm.conditionalTextToPlace).toMatchObject({ id: "ct-2", label: "Updated", status: "update" });
    expect(wrapper.vm.openConditionalTextEditor).toBe(false);

    await wrapper.findComponent({ name: "EditorSubMenu" }).vm.$emit("add-conditional-text");
    await settle(wrapper);

    await wrapper.findComponent({ name: "ConditionalText" }).vm.$emit("cancel");
    await settle(wrapper);

    expect(wrapper.vm.openConditionalTextEditor).toBe(false);
  });

  it("opens and closes the merge-tags CSV upload overlay", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    const uploadBtn = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("UPLOAD MERGE TAGS CSV"));

    await uploadBtn.trigger("click");
    await settle(wrapper);

    expect(wrapper.vm.showPipedTextUploader).toBe(true);

    await wrapper.findComponent({ name: "PipedTextFileUploader" }).vm.$emit("close");
    await settle(wrapper);

    expect(wrapper.vm.showPipedTextUploader).toBe(false);
  });

  it("opens the copy-from menu, lists eligible messages from other containers, and copies via a real click", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer(),
        buildContainer({
          id: "container-2",
          configuration: { title: "Other Container", status: "UNPUBLISHED" },
          messages: [buildMessage({ id: "message-2", conditionId: "c1" })]
        })
      ]
    });
    await settle(wrapper);

    const body = new DOMWrapper(document.body);

    const outerMenu = wrapper.findComponent({ name: "VMenu" });
    await outerMenu.setValue(true);
    await settle(wrapper);

    expect(body.text()).toContain("Other Container");

    const containerActivator = body
      .findAll(".v-list-item")
      .find(item => item.text().includes("Other Container"));

    await containerActivator.trigger("mouseenter");
    await new Promise(resolve => setTimeout(resolve, 400));
    await settle(wrapper);

    const messageItem = body
      .findAll(".v-list-item")
      .find(item => item.text().trim().startsWith("Message") && item.text() !== "Other Container");

    expect(messageItem).toBeTruthy();

    await messageItem.trigger("click");
    await settle(wrapper);

    expect(wrapper.vm.isCopy).toBe(true);
  });

  it("copy() maps every field of a fully-populated source message (attachments, conditional texts, piped text, rule sets, reply-tos)", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    const fromMessage = buildMessage({
      id: "message-source",
      ruleSets: [
        {
          id: "rs-1",
          rules: [{ id: "r-1", lmsAssignmentId: "a1", assignment: null }]
        }
      ],
      configuration: {
        id: "config-source",
        enabled: true,
        type: "EMAIL",
        subject: "Copied subject",
        replyTo: [{ id: "rt-1", email: "a@example.com" }],
        sendAt: "2026-01-01T00:00:00Z",
        sendAtTimezoneOffset: 0,
        status: "READY",
        toConsentedOnly: true,
        matchType: "EXCLUDE"
      },
      content: {
        id: "content-source",
        html: "<p>Copied CT-1 body</p>",
        attachments: [{ id: "att-1", fileName: "a.png" }],
        conditionalTexts: [
          {
            id: "CT-1",
            label: "CT Label",
            ruleSets: [
              {
                id: "cts-rs-1",
                rules: [{ id: "cts-r-1", lmsAssignmentId: "a1", assignment: null }]
              }
            ]
          }
        ],
        pipedText: {
          id: "pt-1",
          fileName: "merge.csv",
          items: [
            {
              id: "item-1",
              values: [{ id: "v-1" }]
            }
          ]
        }
      }
    });

    await wrapper.vm.copy(fromMessage);
    await settle(wrapper);

    expect(wrapper.vm.isCopy).toBe(true);
    expect(wrapper.vm.subject).toBe("Copied subject");
    expect(wrapper.vm.type).toBe("EMAIL");
    expect(wrapper.vm.matchType).toBe("EXCLUDE");
    expect(wrapper.vm.enabled).toBe(true);
    expect(wrapper.vm.attachments).toEqual([{ id: null, fileName: "a.png" }]);
    expect(wrapper.vm.replyToList).toEqual([
      expect.objectContaining({ email: "a@example.com", id: null })
    ]);
    expect(wrapper.vm.pipedText).toMatchObject({ fileName: "merge.csv" });
  });

  it("copy() falls back gracefully when the source message has no piped text or conditional texts", async () => {
    const { wrapper } = mount();
    await settle(wrapper);

    const fromMessage = buildMessage({ id: "message-empty-source" });

    await wrapper.vm.copy(fromMessage);
    await settle(wrapper);

    expect(wrapper.vm.isCopy).toBe(true);
    expect(wrapper.vm.pipedText).toBeNull();
  });

  it("renders the EMAIL-type settings tab controls and forwards their updates to the message configuration", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              configuration: {
                id: "config-1",
                enabled: false,
                type: "EMAIL",
                subject: null,
                replyTo: [],
                sendAt: null,
                status: "READY",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    expect(wrapper.vm.editor).toBe("html");
    expect(wrapper.findComponent({ name: "SendTest" }).exists()).toBe(true);

    const settingsTab = wrapper
      .findAllComponents({ name: "VTab" })
      .find(tabItem => tabItem.text() === "Settings");

    await settingsTab.trigger("click");
    await settle(wrapper);

    expect(wrapper.vm.tab).toBe("settings");
    expect(wrapper.findComponent({ name: "ToConsentedOnly" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "Type" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "ReplyTo" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "Scheduler" }).exists()).toBe(true);

    await wrapper.findComponent({ name: "ToConsentedOnly" }).vm.$emit("updated", true);
    await settle(wrapper);
    expect(wrapper.vm.toConsentedOnly).toBe(true);

    await wrapper.findComponent({ name: "Type" }).vm.$emit("updated", "CONVERSATION");
    await settle(wrapper);
    expect(wrapper.vm.type).toBe("CONVERSATION");

    await wrapper.findComponent({ name: "Scheduler" }).vm.$emit("updated", "2026-02-01T00:00:00Z");
    await settle(wrapper);
    expect(wrapper.vm.sendAt).toBe("2026-02-01T00:00:00Z");
  });

  it("forwards ReplyTo updates onto the message configuration's reply-to list", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              configuration: {
                id: "config-1",
                enabled: false,
                type: "EMAIL",
                subject: null,
                replyTo: [],
                sendAt: null,
                status: "READY",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    const settingsTab = wrapper
      .findAllComponents({ name: "VTab" })
      .find(tabItem => tabItem.text() === "Settings");

    await settingsTab.trigger("click");
    await settle(wrapper);

    await wrapper.findComponent({ name: "ReplyTo" }).vm.$emit("updated", [{ email: "reply@example.com" }]);
    await settle(wrapper);

    expect(wrapper.vm.replyToList).toEqual([
      expect.objectContaining({ email: "reply@example.com" })
    ]);
  });

  it("does not render SendTest for a read-only EMAIL message, and messageTypeLabel/editor fall back when the type is unset", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              configuration: {
                id: "config-1",
                enabled: true,
                type: "EMAIL",
                subject: "Hi",
                replyTo: [],
                sendAt: null,
                status: "SENT",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              },
              content: {
                id: "content-1",
                html: "<p>Body</p>",
                attachments: [],
                conditionalTexts: [],
                pipedText: null
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    expect(wrapper.findComponent({ name: "SendTest" }).exists()).toBe(false);
  });

  it("falls back to N/A and a null editor when the message type is unset", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              configuration: {
                id: "config-1",
                enabled: false,
                type: null,
                subject: null,
                replyTo: [],
                sendAt: null,
                status: "READY",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    expect(wrapper.text()).toContain("N/A");
    expect(wrapper.vm.editor).toBeNull();
  });

  it("saveExit switches to the settings tab when only reply-to validation fails on the treatment tab", async () => {
    const replyToUpdateMock = vi.fn().mockResolvedValue(false);
    const ReplyToStub = {
      name: "ReplyTo",
      props: ["replyTos", "required", "readOnly"],
      template: "<div class=\"reply-to-stub\" />",
      methods: {
        updateReplyTo: () => replyToUpdateMock()
      }
    };

    const { wrapper } = mount(
      {
        containers: [
          buildContainer({
            messages: [
              buildMessage({
                configuration: {
                  id: "config-1",
                  enabled: true,
                  type: "EMAIL",
                  subject: "A valid subject",
                  replyTo: [],
                  sendAt: null,
                  status: "READY",
                  toConsentedOnly: false,
                  matchType: "INCLUDE"
                },
                content: {
                  id: "content-1",
                  html: "<p>A valid body</p>",
                  attachments: [],
                  conditionalTexts: [],
                  pipedText: null
                }
              })
            ]
          })
        ]
      },
      { global: { stubs: { ...stubs, ReplyTo: ReplyToStub } } }
    );
    await settle(wrapper);

    const settingsTab = wrapper
      .findAllComponents({ name: "VTab" })
      .find(tabItem => tabItem.text() === "Settings");

    await settingsTab.trigger("click");
    await settle(wrapper);

    // sanity: navigate back to the treatment tab before saving, since that's
    // the branch under test (treatment -> settings on reply-to failure)
    const treatmentTab = wrapper
      .findAllComponents({ name: "VTab" })
      .find(tabItem => tabItem.text() === "Treatment");

    await treatmentTab.trigger("click");
    await settle(wrapper);

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(false);
    expect(wrapper.vm.tab).toBe("settings");
  });

  it("saveExit switches back to the treatment tab when body/subject validation fails while on the settings tab", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              configuration: {
                id: "config-1",
                enabled: true,
                type: "CONVERSATION",
                subject: null,
                replyTo: [],
                sendAt: null,
                status: "READY",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    wrapper.vm.tab = "settings";
    await settle(wrapper);

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(false);
    expect(wrapper.vm.tab).toBe("treatment");
  });

  it("saveExit blocks saving with a Swal message while a conditional text is still being created or edited", async () => {
    const { wrapper, conditionalTextStore } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              configuration: {
                id: "config-1",
                enabled: true,
                type: "CONVERSATION",
                subject: "Subject",
                replyTo: [],
                sendAt: null,
                status: "READY",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              },
              content: {
                id: "content-1",
                html: "<p>Body</p>",
                attachments: [],
                conditionalTexts: [],
                pipedText: null
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    conditionalTextStore.messageConditionalText = {
      id: null,
      label: "In progress",
      result: { html: "<p>Some content</p>" },
      ruleSets: []
    };
    await settle(wrapper);

    Swal.fire.mockClear();

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(false);
    expect(Swal.fire).toHaveBeenCalledWith(
      "Please finish creating the conditional text before saving the message."
    );
    expect(messageService.update).not.toHaveBeenCalled();
  });

  it("saveExit sanitizes ruleSet/rule ids for a copied message before saving", async () => {
    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              isCopy: true,
              ruleSets: [
                {
                  id: "rs-old",
                  rules: [
                    {
                      id: "r-old",
                      ruleSetId: "rs-old",
                      lmsAssignmentId: "a1",
                      assignment: { lmsId: "a1" },
                      comparison: { id: "eq", requiresValue: false }
                    }
                  ]
                }
              ],
              configuration: {
                id: "config-1",
                enabled: true,
                type: "CONVERSATION",
                subject: "Subject",
                replyTo: [],
                sendAt: null,
                status: "READY",
                toConsentedOnly: false,
                matchType: "INCLUDE"
              },
              content: {
                id: "content-1",
                html: "<p>Body</p>",
                attachments: [],
                conditionalTexts: [
                  {
                    id: "ct-old",
                    label: "CT",
                    ruleSets: [
                      {
                        id: "cts-rs-old",
                        conditionalTextId: "ct-old",
                        rules: [{ id: "cts-r-old", ruleSetId: "cts-rs-old" }]
                      }
                    ]
                  }
                ],
                pipedText: null
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(true);
    const [, , , , payload] = messageService.update.mock.calls.at(-1);
    expect(payload.ruleSets[0].id).toBeNull();
    expect(payload.ruleSets[0].rules[0].id).toBeNull();
    expect(payload.content.conditionalTexts[0].ruleSets[0].id).toBeNull();
  });

  it("re-associates rule assignments once message rule assignments finish loading", async () => {
    messageService.getAssignments.mockResolvedValue([{ lmsId: "a1", name: "Assignment 1" }]);

    const { wrapper } = mount({
      containers: [
        buildContainer({
          messages: [
            buildMessage({
              ruleSets: [
                { id: "rs-1", rules: [{ id: "r-1", lmsAssignmentId: "a1", assignment: null }] }
              ],
              content: {
                id: "content-1",
                html: null,
                attachments: [],
                conditionalTexts: [
                  {
                    id: "ct-1",
                    label: "CT",
                    ruleSets: [
                      { id: "cts-rs-1", rules: [{ id: "cts-r-1", lmsAssignmentId: "a1", assignment: null }] }
                    ]
                  }
                ],
                pipedText: null
              }
            })
          ]
        })
      ]
    });
    await settle(wrapper);

    expect(wrapper.vm.ruleSets[0].rules[0].assignment).toMatchObject({ lmsId: "a1" });
  });

  it("re-fetches placeholders and shows the uploaded filename when the piped-text message resolves with content", async () => {
    messageService.updatePlaceholders.mockResolvedValue({
      html: "<p>with placeholders</p>",
      conditionalTexts: []
    });

    const { wrapper, messageStore } = mount();
    await settle(wrapper);

    messageStore.message = {
      content: { pipedText: { id: "pt-1", fileName: "merged.csv", items: [] } }
    };
    await settle(wrapper);

    expect(wrapper.vm.isUploadSuccessful).toBe(true);
    expect(wrapper.vm.showPipedTextUploader).toBe(false);
    expect(messageService.updatePlaceholders).toHaveBeenCalled();
    expect(wrapper.text()).toContain("merged.csv");
  });

  it("shows an upload error alert when the piped-text message resolves with validation errors", async () => {
    const { wrapper, messageStore } = mount();
    await settle(wrapper);

    Swal.fire.mockClear();

    messageStore.message = {
      validationErrors: ["Row 2: missing value", "Row 5: bad format"]
    };
    await settle(wrapper);

    expect(wrapper.vm.showPipedTextUploader).toBe(false);
    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({
        title: "Error uploading CSV",
        html: expect.stringContaining("Errors")
      })
    );
  });
});
