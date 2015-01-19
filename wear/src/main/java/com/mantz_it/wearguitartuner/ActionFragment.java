package com.mantz_it.wearguitartuner;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wearable.view.CircledImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * <h1>Action Fragment by Destil</h1>
 *
 * Module:      ActionFragment.java
 * Description: Action button fragment implemented by Destil
 * Source:      http://stackoverflow.com/questions/24969086/is-there-an-easy-way-to-create-an-action-button-fragment
 *
 * @author Destil (http://stackoverflow.com/users/560358/destil)
 */

public class ActionFragment extends Fragment implements View.OnClickListener {

	private static Listener mListener;
	private CircledImageView vIcon;
	private TextView vLabel;

	public static ActionFragment create(int iconResId, int labelResId, Listener listener) {
		mListener = listener;
		ActionFragment fragment = new ActionFragment();
		Bundle args = new Bundle();
		args.putInt("ICON", iconResId);
		args.putInt("LABEL", labelResId);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_action, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		vIcon = (CircledImageView) view.findViewById(R.id.civ_actionFragment_icon);
		vLabel = (TextView) view.findViewById(R.id.tv_actionFragment_label);
		vIcon.setImageResource(getArguments().getInt("ICON"));
		vLabel.setText(getArguments().getInt("LABEL"));
		view.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		mListener.onActionPerformed();
	}

	public interface Listener {
		public void onActionPerformed();
	}
}