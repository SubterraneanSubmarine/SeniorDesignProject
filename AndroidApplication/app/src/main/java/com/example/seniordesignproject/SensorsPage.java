package com.example.seniordesignproject;
/*
ECE 4800 - Senior Project
David Carlson and Bryce Martin

Inspiration for the design and coding of this android app originate from:
https://app.pluralsight.com/library/courses/android-fundamentals-fragments/table-of-contents
                -- Sriyank Siddhartha
                For his teaching about Fragments and UI management

 https://app.pluralsight.com/
                      library/courses/android-studio-connected-app-building-first/table-of-contents
                -- Simone Alessandria
                For his teaching how to do Async internet/server fetches

 https://developer.android.com/guide/topics/ui/layout/recyclerview
 https://stackoverflow.com/questions/26621060/display-a-recyclerview-in-fragment
 https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
 */
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;



// TODO This page is a work in progress...
// This is 90% a copy and amalgamation of StackOverflow and Google Developer documentation
// Still learning how this works.


public class SensorsPage extends Fragment {
    private static final String TAG = "S.D.A.SensorsPage";  // Used for debug output

    // We allocate memory for the RecyclerView -- Scrolling list
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;


    // When a Fragment is created, then added to a contentmanager/switcher, this method is called
    @Override
    public void onAttach(Context context){
        super.onAttach(context);
//        Log.d(TAG, "onAttach()");
    }

    // When a Fragment is called and its visibility is set to 'Show" thi is what is called
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // This object represents content and data currently viewed on screen
        View view = inflater.inflate(R.layout.sensors_layout, container, false);

        // Inform our Recycler view where it will put/show our data
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);  // Optional
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // Here is sample data that will be shown in the list
        String[] myData2 = {"This", "IS", "Xbee", "Sensor",
                "Stats", "That", "You", "can",
                "Read", "Here", "in", "A",
                "Scrolling", "list", "'RecyclerView'", "Yep..."};

        // An 'Adapter'
        mAdapter = new MyAdapter(myData2);
        recyclerView.setAdapter(mAdapter);


        return view;
    }


    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<String> mData;
        private LayoutInflater mInflater;
//        private ItemClickListener mClickListener;

        String[] mData2;

        // data is passed into the constructor
        MyAdapter(String[] data) {
            //this.mInflater = LayoutInflater.from(context);
            this.mData2 = data;
        }

        // inflates the row layout from xml when needed
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.sensors_recycler_rows, parent, false);
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sensors_recycler_rows, parent, false);
            return new ViewHolder(view);
        }

        // binds the data to the TextView in each row
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String animal = mData2[position];
            holder.myTextView.setText(animal);
        }

        // total number of rows
        @Override
        public int getItemCount() {
            return  mData2.length;
        }


        // stores and recycles views as they are scrolled off screen
        public class ViewHolder extends RecyclerView.ViewHolder/*implements View.OnClickListener*/ {
            TextView myTextView;

            ViewHolder(View itemView) {
                super(itemView);
                myTextView = itemView.findViewById(R.id.recyclerRow);
//                itemView.setOnClickListener(this);
            }

//            @Override
//            public void onClick(View view) {
//                if (mClickListener != null) {
//                    mClickListener.onItemClick(view, getAdapterPosition());
//                }
//            }
        }

        // convenience method for getting data at click position
        String getItem(int id) {
            return mData.get(id);
        }

        // allows clicks events to be caught
//        void setClickListener(ItemClickListener itemClickListener) {
//            this.mClickListener = itemClickListener;
//        }

        // parent activity will implement this method to respond to click events
//        public interface ItemClickListener {
//            void onItemClick(View view, int position);
//        }
    }
}
