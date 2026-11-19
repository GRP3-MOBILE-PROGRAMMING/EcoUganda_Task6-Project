package com.example.ecouganda.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ecouganda.R;
import com.example.ecouganda.models.Report;
import com.example.ecouganda.navigation.DeepLinks;
import com.example.ecouganda.services.DataService;

/**
 * Districts / Reports screen.
 *
 * <ul>
 *   <li>No {@code reportId} argument: the list of urgent reports.</li>
 *   <li>With {@code reportId}: the detail of that report. Push notifications
 *       and ecouganda://reports/{id} deep links open this mode.</li>
 * </ul>
 *
 * The id lives in the fragment arguments, so Android restores it automatically
 * after rotation or process death.
 */
public class ReportsFragment extends Fragment {

    public static final String ARG_REPORT_ID = DeepLinks.EXTRA_REPORT_ID;
    public static final String TAG = "reports";

    /** Implemented by the hosting activity. */
    public interface Host {
        void openReport(String reportId);

        void showReportsList();
    }

    public static ReportsFragment newInstance(@Nullable String reportId) {
        ReportsFragment f = new ReportsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REPORT_ID, reportId);
        f.setArguments(args);
        return f;
    }

    /** Report shown by this fragment, or null for the list. */
    @Nullable
    public String getReportId() {
        Bundle args = getArguments();
        return args == null ? null : args.getString(ARG_REPORT_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        int layout = getReportId() == null ? R.layout.fragment_reports : R.layout.fragment_report_detail;
        return inflater.inflate(layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getReportId() == null) {
            bindList(view);
        } else {
            bindDetail(view, getReportId());
        }
    }

    private void bindList(View view) {
        View mabira = view.findViewById(R.id.report_mabira);
        if (mabira != null) mabira.setOnClickListener(v -> host().openReport("RPT-2048"));
        View dumping = view.findViewById(R.id.report_dumping);
        if (dumping != null) dumping.setOnClickListener(v -> host().openReport("RPT-2041"));
    }

    private void bindDetail(View view, String reportId) {
        Report report = DataService.getInstance().getReportById(reportId);
        if (report == null) return; // MainActivity never opens an unknown id

        setText(view, R.id.report_detail_title, report.getTitle());
        setText(view, R.id.report_detail_id, getString(R.string.report_id_format, report.getId()));
        setText(view, R.id.report_detail_location,
                getString(R.string.report_location_format, report.getLocation(), report.getDistrict()));
        setText(view, R.id.report_detail_received, report.getTimestamp());
        setText(view, R.id.report_detail_status, report.getStatus());
        setText(view, R.id.report_detail_notes, report.getDescription());

        View back = view.findViewById(R.id.btn_back_to_reports);
        if (back != null) back.setOnClickListener(v -> host().showReportsList());

        View assign = view.findViewById(R.id.assign_investigation);
        if (assign != null) assign.setOnClickListener(v -> Toast.makeText(requireContext(),
                getString(R.string.toast_investigation_assigned, report.getId()), Toast.LENGTH_SHORT).show());

        View map = view.findViewById(R.id.view_location);
        if (map != null) map.setOnClickListener(v -> Toast.makeText(requireContext(),
                getString(R.string.toast_opening_map, report.getLocation()), Toast.LENGTH_SHORT).show());
    }

    private Host host() {
        return (Host) requireActivity();
    }

    private static void setText(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }
}
