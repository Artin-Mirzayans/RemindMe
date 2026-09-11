import formatPhoneNumber from "./formatPhoneNumber";

describe("formatPhoneNumber", () => {
  it("leaves a short run of digits alone", () => {
    expect(formatPhoneNumber("555")).toBe("555");
  });

  it("brackets the area code once there are four digits", () => {
    expect(formatPhoneNumber("5558")).toBe("(555) 8");
  });

  it("formats a complete number", () => {
    expect(formatPhoneNumber("5558675309")).toBe("(555) 867-5309");
  });

  it("ignores punctuation the user typed", () => {
    expect(formatPhoneNumber("(555) 867-5309")).toBe("(555) 867-5309");
  });

  it("stops at ten digits", () => {
    expect(formatPhoneNumber("55586753091234")).toBe("(555) 867-5309");
  });

  it("passes empty input straight through", () => {
    expect(formatPhoneNumber("")).toBe("");
  });
});
