import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";

const route = {
  params: { experimentId: "1" },
  query: { exposureId: "exposure-1", mode: "NEW", version: "MULTIPLE" }
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
  messageContainerService: {
    create: vi.fn(),
    update: vi.fn()
  }
}));

import { createPinia, setActivePinia } from "pinia";
import Swal from "sweetalert2";

import { messageContainerService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import MessageContainer from "./MessageContainer.vue";

import { experiment as experimentModule } from "@/store/experiment.module";
import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { alert as alertModule } from "@/store/alert.module";

const ReplyToStub = {
  name: "ReplyTo",
  props: ["replyTos", "readOnly", "required"],
  emits: ["updated"],
  template: "<div class=\"reply-to-stub\" />",
  methods: {
    updateReplyTo() {
      return true;
    }
  }
};

const stubs = {
  ReplyTo: ReplyToStub,
  Scheduler: true,
  ToConsentedOnly: true,
  Type: true
};

function seedStores({ containers = [] } = {}) {
  const pinia = createPinia();
  setActivePinia(pinia);

  experimentModule().experiments = [
    {
      experimentId: 1,
      createdByEmail: "creator@example.com",
      conditions: []
    }
  ];

  const containerStore = messagingMessageContainerModule();
  containerStore.messageContainers = containers;

  alertModule();

  return { pinia, containerStore };
}

async function mount(overrides = {}, seedOptions = {}) {
  Object.assign(route.query, {
    mode: "NEW",
    version: "MULTIPLE",
    containerId: undefined,
    ...overrides
  });

  const { pinia, containerStore } = seedStores(seedOptions);

  const wrapper = mountComponent(MessageContainer, {
    pinia,
    global: { stubs }
  });

  await wrapper.vm.$nextTick();

  return { wrapper, containerStore };
}

describe("MessageContainer", () => {
  beforeEach(() => {
    document.body.innerHTML =
      '<div class="steps-container-col col-md-6"></div>' +
      '<div class="experiment-steps__body pt-4"></div>';

    vi.clearAllMocks();
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("renders an empty title field for a new container", async () => {
    const { wrapper } = await mount();

    const titleField = wrapper.findComponent({ name: "VTextField" });
    expect(titleField.props("modelValue")).toBe("");
  });

  it("loads the existing container's title when editing", async () => {
    const { wrapper } = await mount(
      { mode: "EDIT", containerId: "container-1" },
      {
        containers: [
          {
            id: "container-1",
            configuration: {
              id: "config-1",
              title: "Existing Container",
              status: "UNPUBLISHED",
              type: "EMAIL",
              toConsentedOnly: false,
              replyTo: [],
              sendAt: null
            }
          }
        ]
      }
    );

    const titleField = wrapper.findComponent({ name: "VTextField" });
    expect(titleField.props("modelValue")).toBe("Existing Container");
  });

  it("blocks saving and alerts the user when required fields are missing", async () => {
    const { wrapper } = await mount();

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(false);
    expect(Swal.fire).toHaveBeenCalledWith(
      "Please complete all required sections."
    );
    expect(messageContainerService.create).not.toHaveBeenCalled();
  });

  it("creates then updates a brand-new container on a successful save", async () => {
    const { wrapper } = await mount();

    messageContainerService.create.mockResolvedValue({
      id: "new-container",
      configuration: {
        id: "new-config",
        title: null,
        status: "UNPUBLISHED",
        type: "NONE",
        toConsentedOnly: false,
        replyTo: [],
        sendAt: null
      }
    });
    messageContainerService.update.mockResolvedValue({});

    const titleField = wrapper.findComponent({ name: "VTextField" });
    await titleField.setValue("Brand New Container");

    const typeStub = wrapper.findComponent({ name: "Type" });
    await typeStub.vm.$emit("updated", "EMAIL");

    const schedulerStub = wrapper.findComponent({ name: "Scheduler" });
    await schedulerStub.vm.$emit("updated", "2026-08-01T10:00:00.000Z");

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(true);
    expect(messageContainerService.create).toHaveBeenCalledWith(
      1,
      "exposure-1",
      false
    );
    expect(messageContainerService.update).toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 1 }
    });
  });

  it("updates (without creating) an existing container on a successful save", async () => {
    const { wrapper } = await mount(
      { mode: "EDIT", containerId: "container-1" },
      {
        containers: [
          {
            id: "container-1",
            configuration: {
              id: "config-1",
              title: "Existing Container",
              status: "UNPUBLISHED",
              type: "EMAIL",
              toConsentedOnly: false,
              replyTo: [],
              sendAt: "2026-08-01T10:00:00.000Z"
            }
          }
        ]
      }
    );

    messageContainerService.update.mockResolvedValue({});

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(true);
    expect(messageContainerService.create).not.toHaveBeenCalled();
    expect(messageContainerService.update).toHaveBeenCalledWith(
      1,
      "exposure-1",
      "container-1",
      expect.objectContaining({ id: "container-1" })
    );
    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 1 }
    });
  });

  it("disables the title field for a read-only (sent) container", async () => {
    const { wrapper } = await mount(
      { mode: "EDIT", containerId: "container-1" },
      {
        containers: [
          {
            id: "container-1",
            configuration: {
              id: "config-1",
              title: "Existing Container",
              status: "SENT",
              type: "EMAIL",
              toConsentedOnly: false,
              replyTo: [],
              sendAt: "2026-08-01T10:00:00.000Z"
            }
          }
        ]
      }
    );

    const titleField = wrapper.findComponent({ name: "VTextField" });
    expect(titleField.props("disabled")).toBe(true);
  });
});
