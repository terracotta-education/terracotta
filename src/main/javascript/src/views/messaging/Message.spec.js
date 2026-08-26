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
import { flushPromises } from "@vue/test-utils";
import Swal from "sweetalert2";

import { messageService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import Message from "./Message.vue";

import { condition as conditionModule } from "@/store/condition.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { message as messagingMessageModule } from "@/store/messaging/message.module";
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
  alertModule();

  return { pinia, containerStore, messageStore };
}

function mount(seedOptions = {}) {
  const { pinia, containerStore, messageStore } = seedStores(seedOptions);

  const wrapper = mountComponent(Message, {
    pinia,
    global: { stubs }
  });

  return { wrapper, containerStore, messageStore };
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
});
