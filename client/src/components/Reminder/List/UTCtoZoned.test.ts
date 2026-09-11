import UTCToZoned from "./UTCtoZoned";

describe("UTCToZoned", () => {
  it("formats the date and time for display", () => {
    const { formattedDate, formattedTime } = UTCToZoned("2030-01-01T09:00:00Z");

    expect(formattedDate).toBe("Jan 1");
    expect(formattedTime).toBe("9:00 AM");
  });

  it("uses a twelve hour clock in the afternoon", () => {
    expect(UTCToZoned("2030-01-01T21:30:00Z").formattedTime).toBe("9:30 PM");
  });

  it("describes a future reminder as being ahead", () => {
    const soon = new Date(Date.now() + 3 * 60 * 60 * 1000).toISOString();

    expect(UTCToZoned(soon).relative).toMatch(/in /);
  });

  it("describes a past reminder as being behind", () => {
    const earlier = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString();

    expect(UTCToZoned(earlier).relative).toMatch(/ago/);
  });
});
