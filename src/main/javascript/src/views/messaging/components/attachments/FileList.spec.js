import { describe, expect, it, vi, beforeEach } from "vitest";

vi.mock("@/services", () => ({
  messageContentAttachmentService: {
    getAll: vi.fn()
  },
  messageContainerService: {}
}));

import { createPinia, setActivePinia } from "pinia";

import { messageContentAttachmentService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import FileList from "./FileList.vue";
import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";

function buildContainer(overrides = {}) {
  return {
    id: "container-1",
    myFilesUrl: "https://canvas.example.com/files",
    messages: [
      {
        id: "message-1",
        content: {
          attachments: [
            { lmsId: "f1", displayName: "File One" }
          ]
        }
      }
    ],
    ...overrides
  };
}

const baseProps = {
  experimentId: 1,
  exposureId: "exposure-1",
  containerId: "container-1",
  messageId: "message-1",
  contentId: "content-1"
};

describe("FileList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    messageContentAttachmentService.getAll.mockResolvedValue([
      { lmsId: "f1", displayName: "File One" },
      { lmsId: "f2", filename: "file2.txt" }
    ]);
  });

  async function openMenu(wrapper) {
    await wrapper.findComponent({ name: "VBtn" }).trigger("click");
    await flushPromises();
  }

  function mountWithContainer(props = {}, containerOverrides = {}) {
    const pinia = createPinia();
    setActivePinia(pinia);

    const containerStore = messagingMessageContainerModule();
    containerStore.messageContainers = [buildContainer(containerOverrides)];

    return mountComponent(FileList, {
      pinia,
      props: { ...baseProps, ...props }
    });
  }

  it("fetches the attachment list on mount with the correct payload", () => {
    mountWithContainer();

    expect(messageContentAttachmentService.getAll).toHaveBeenCalledWith(
      1,
      "exposure-1",
      "container-1",
      "message-1",
      "content-1"
    );
  });

  it("shows the count of already-selected files on the activator button", async () => {
    const wrapper = mountWithContainer();
    await flushPromises();

    expect(wrapper.text()).toContain("(1)");
  });

  it("lists both the already-attached files and the other files available to attach", async () => {
    const wrapper = mountWithContainer();
    await flushPromises();
    await openMenu(wrapper);

    const checkboxes = wrapper.findAllComponents({ name: "VCheckbox" });
    expect(checkboxes).toHaveLength(2);

    const labels = checkboxes.map(checkbox => checkbox.props("label"));
    expect(labels).toEqual(
      expect.arrayContaining(["File One", "file2.txt"])
    );
  });

  it("adds a file to the message's attachments when its checkbox is checked", async () => {
    const wrapper = mountWithContainer();
    await flushPromises();
    await openMenu(wrapper);

    const checkboxes = wrapper.findAllComponents({ name: "VCheckbox" });
    const fileTwoCheckbox = checkboxes.find(
      checkbox => checkbox.props("label") === "file2.txt"
    );

    await fileTwoCheckbox.find("input").setValue(true);

    const containerStore = messagingMessageContainerModule();
    const attachments =
      containerStore.messageContainers[0].messages[0].content.attachments;

    expect(attachments.map(file => file.lmsId)).toEqual(
      expect.arrayContaining(["f1", "f2"])
    );
  });

  it("removes a file from the message's attachments when its checkbox is unchecked", async () => {
    const wrapper = mountWithContainer();
    await flushPromises();
    await openMenu(wrapper);

    const checkboxes = wrapper.findAllComponents({ name: "VCheckbox" });
    const fileOneCheckbox = checkboxes.find(
      checkbox => checkbox.props("label") === "File One"
    );

    await fileOneCheckbox.find("input").setValue(false);

    const containerStore = messagingMessageContainerModule();
    const attachments =
      containerStore.messageContainers[0].messages[0].content.attachments;

    expect(attachments.map(file => file.lmsId)).not.toContain("f1");
  });

  it("re-fetches the attachment list when Refresh File List is clicked", async () => {
    const wrapper = mountWithContainer();
    await flushPromises();
    await openMenu(wrapper);
    messageContentAttachmentService.getAll.mockClear();

    const refreshButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("Refresh File List"));

    await refreshButton.trigger("click");
    await flushPromises();

    expect(messageContentAttachmentService.getAll).toHaveBeenCalledTimes(1);
  });

  it("disables the checkboxes and refresh button when readOnly", async () => {
    const wrapper = mountWithContainer({ readOnly: true });
    await flushPromises();
    await openMenu(wrapper);

    const refreshButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("Refresh File List"));
    expect(refreshButton.props("disabled")).toBe(true);

    const checkboxes = wrapper.findAllComponents({ name: "VCheckbox" });
    checkboxes.forEach(checkbox => {
      expect(checkbox.props("disabled")).toBe(true);
    });
  });

  it("does not render the checkbox inside the prepend slot, which doesn't get flex-constrained to the list item's width and let long filenames overflow past the menu instead of wrapping", async () => {
    const wrapper = mountWithContainer();
    await flushPromises();
    await openMenu(wrapper);

    // the menu's content teleports outside wrapper's own DOM subtree, like the
    // "links to the container's My Files URL" test below has to account for too.
    expect(document.querySelector(".v-list-item__prepend")).toBeNull();
  });

  it("links to the container's My Files URL", async () => {
    const wrapper = mountWithContainer();
    await flushPromises();
    await openMenu(wrapper);

    const link = document.querySelector("a");
    expect(link.getAttribute("href")).toBe(
      "https://canvas.example.com/files"
    );
  });
});

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve));
}
