package com.example.ecouganda.performance;

/**
 * Journey 3: the two-step "Report Issue" bottom sheet.
 *
 * <pre>
 * report_issue_sheet_open        tap "Report Issue" -> first sheet STATE_EXPANDED
 * report_issue_sheet_transition  tap "Next"         -> second sheet STATE_EXPANDED
 * </pre>
 *
 * The transition trace isolates the sheet-to-sheet swap (dismiss + inflate +
 * expand animation), which is where low-end phones usually stutter.
 */
public final class ReportIssueSheetJourney {

    private final TraceContext ctx;

    private PerfTrace openTrace;
    private PerfTrace transitionTrace;
    private long openStartedAt;
    private long transitionStartedAt;
    private boolean openDone;
    private boolean finished;

    public ReportIssueSheetJourney(TraceContext ctx) {
        this.ctx = ctx;
    }

    public synchronized void onOpenRequested() {
        if (finished || openTrace != null) return;
        openTrace = ctx.startTrace(TraceNames.REPORT_ISSUE_SHEET_OPEN);
        openStartedAt = ctx.clock.nowMillis();
    }

    /** First sheet reached STATE_EXPANDED (BottomSheetCallback.onStateChanged). */
    public synchronized void onFirstSheetExpanded() {
        if (finished || openTrace == null || openDone) return;
        openTrace.putMetric(TraceNames.M_SHEET_RENDER_MS, ctx.clock.nowMillis() - openStartedAt);
        openTrace.putAttribute(TraceNames.A_OUTCOME, "shown");
        openTrace.stop();
        openDone = true;
    }

    /** "Next" tapped on the first sheet. */
    public synchronized void onNextTapped() {
        if (finished || !openDone || transitionTrace != null) return;
        transitionTrace = ctx.startTrace(TraceNames.REPORT_ISSUE_SHEET_TRANSITION);
        transitionStartedAt = ctx.clock.nowMillis();
    }

    /** Second sheet reached STATE_EXPANDED. Completes the journey. */
    public synchronized void onSecondSheetExpanded() {
        if (finished || transitionTrace == null) return;
        transitionTrace.putMetric(TraceNames.M_TRANSITION_MS, ctx.clock.nowMillis() - transitionStartedAt);
        transitionTrace.putAttribute(TraceNames.A_OUTCOME, "completed");
        transitionTrace.stop();
        finished = true;
    }

    /** Sheet dismissed (swipe down / back) before the flow completed. */
    public synchronized void onDismissed() {
        if (finished) return;
        if (openTrace != null && !openDone) {
            openTrace.putAttribute(TraceNames.A_OUTCOME, "dismissed");
            openTrace.stop();
        }
        if (transitionTrace != null) {
            transitionTrace.putAttribute(TraceNames.A_OUTCOME, "dismissed");
            transitionTrace.stop();
        }
        finished = true;
    }
}
