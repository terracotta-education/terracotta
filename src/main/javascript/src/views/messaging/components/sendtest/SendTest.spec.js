import { afterEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

import { mountComponent } from "@/test-utils/mount";
import { container as useContainerStore } from "@/store/messaging/container.module";
import { message as useMessageStore } from "@/store/messaging/message.module";
import SendTest from "./SendTest.vue";

const baseProps = {
  experimentId: 1,
  exposureId: "exposure-1",
  containerId: "container-1",
  messageId: "message-1",
  email: "instructor@example.com"
};

const setupStores = () => {
  const pinia = createPinia();
  setActivePinia(pinia);

  useContainerStore().messageContainers = [
    {
      id: "container-1",
      messages: [
        {
          id: "message-1",
          configuration: { subject: "Hello there" },
          content: { html: "<p>Body</p>" }
        }
      ]
    }
  ];
  useMessageStore().sendTest = vi.fn().mockResolvedValue({});

  return pinia;
};

// The panel's body (email address, edit form, send button) lives inside a
// collapsed v-expansion-panel-text, which Vuetify only renders once the
// panel has actually been opened.
const mountSendTest = async props => {
  const pinia = setupStores();
  const wrapper = mountComponent(SendTest, { props: { ...baseProps, ...props }, pinia });

  await wrapper.find(".v-expansion-panel-title").trigger("click");
  await flushPromises();

  return wrapper;
};

const findBtn = (wrapper, text) =>
  wrapper.findAllComponents({ name: "VBtn" }).find(b => b.text() === text);

describe("SendTest", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("renders the current test email address and an enabled Send Test Now button", async () => {
    wrapper = await mountSendTest();

    expect(wrapper.text()).toContain("instructor@example.com");
    expect(findBtn(wrapper, "Send Test Now").props("disabled")).toBe(false);
  });

  it("disables Send Test Now when the email is invalid", async () => {
    wrapper = await mountSendTest({ email: "not-an-email" });

    expect(findBtn(wrapper, "Send Test Now").props("disabled")).toBe(true);
  });

  it("switches to edit mode showing a pre-filled text field when Edit is clicked", async () => {
    wrapper = await mountSendTest();

    await findBtn(wrapper, "Edit").trigger("click");

    const field = wrapper.findComponent({ name: "VTextField" });
    expect(field.exists()).toBe(true);
    expect(field.props("modelValue")).toBe("instructor@example.com");
  });

  it("disables Save while the edited email is invalid, and enables it once valid", async () => {
    wrapper = await mountSendTest();

    await findBtn(wrapper, "Edit").trigger("click");
    await wrapper.findComponent({ name: "VTextField" }).setValue("bad-email");

    expect(findBtn(wrapper, "Save").props("disabled")).toBe(true);

    await wrapper.findComponent({ name: "VTextField" }).setValue("new@example.com");
    expect(findBtn(wrapper, "Save").props("disabled")).toBe(false);

    await findBtn(wrapper, "Save").trigger("click");

    expect(wrapper.findComponent({ name: "VTextField" }).exists()).toBe(false);
    expect(wrapper.text()).toContain("new@example.com");
  });

  it("reverts to the previous email when Cancel is clicked", async () => {
    wrapper = await mountSendTest();

    await findBtn(wrapper, "Edit").trigger("click");
    await wrapper.findComponent({ name: "VTextField" }).setValue("changed@example.com");

    await findBtn(wrapper, "Cancel").trigger("click");

    expect(wrapper.findComponent({ name: "VTextField" }).exists()).toBe(false);
    expect(wrapper.text()).toContain("instructor@example.com");
  });

  it("sends the test message with the container's subject/body and reports success", async () => {
    wrapper = await mountSendTest();

    await findBtn(wrapper, "Send Test Now").trigger("click");
    await flushPromises();

    expect(useMessageStore().sendTest).toHaveBeenCalledWith([
      baseProps.experimentId,
      baseProps.exposureId,
      baseProps.containerId,
      baseProps.messageId,
      {
        to: "instructor@example.com",
        subject: "Hello there",
        message: "<p>Body</p>"
      }
    ]);
    expect(wrapper.text()).toContain("Email sent!");
    expect(wrapper.text()).not.toContain("Sending...");
  });
});
