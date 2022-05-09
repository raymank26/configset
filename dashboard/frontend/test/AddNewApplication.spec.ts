/**
 * @jest-environment jsdom
 */
import {mount} from '@vue/test-utils'
import AddNewApplication from "@/components/AddNewApplication.vue";
import {applicationService} from "@/service/services";
import Mock = jest.Mock;

jest.mock('@/service/services')
let applicationServiceMock = applicationService.createApplication as Mock<any, any>;

beforeEach(() => {
  applicationServiceMock.mockClear()
})

test('application is saved, user redirected to /applications', async () => {
  // given
  let mockRouter = {
    push: jest.fn()
  }
  let wrapper = mount(AddNewApplication, {
    mocks: {
      $router: mockRouter
    }
  })

  applicationServiceMock.mockImplementation(v => {
    expect(v).toEqual("TestApp")
    return Promise.resolve()
  })

  // when
  await wrapper.find("#appName").setValue("TestApp")
  await wrapper.find("form").trigger("submit")

  // then
  expect(wrapper.find("form").classes("was-validated")).toBe(true)
  expect(applicationServiceMock).toBeCalled()
  expect(mockRouter.push).toHaveBeenCalledWith({
    path: "/applications"
  })
});

test('application has invalid name, error is shown', async () => {

  // given
  let wrapper = mount(AddNewApplication, {})

  // when
  await wrapper.find("#appName").setValue("invalid name 29")
  await wrapper.find("form").trigger("submit")

  // then
  expect(wrapper.find("form").classes("was-validated")).toBe(true)
  expect(wrapper.find(".invalid-feedback").isVisible()).toBe(true)
  expect(applicationServiceMock).not.toBeCalled()
})

test('application is empty, error is shown', async () => {
  // given
  let wrapper = mount(AddNewApplication, {})

  // when
  await wrapper.find("form").trigger("submit")

  // then
  expect(wrapper.find("form").classes("was-validated")).toBe(true)
  expect(wrapper.find(".invalid-feedback").isVisible()).toBe(true)
  expect(applicationServiceMock).not.toBeCalled()
})
