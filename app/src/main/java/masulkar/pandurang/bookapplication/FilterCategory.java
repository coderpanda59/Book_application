package masulkar.pandurang.bookapplication;

import android.widget.Filter;

import java.util.ArrayList;

public class FilterCategory extends Filter {

    //arraylist in which we want to search
    ArrayList<ModelCategory> filterList;
    //adapter in which filter need to be implemented
    AdapterCategory adapterCategory;

    //constructor

    public FilterCategory(ArrayList<ModelCategory> filterList,AdapterCategory adapterCategory) {
        this.filterList = filterList;
        this.adapterCategory=adapterCategory;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results=new FilterResults();
        //value should not be null and empty
        if(constraint!=null&& constraint.length()>0){
            //changes to upper case or lower case to avoid case sensitivity
            constraint=constraint.toString().toUpperCase();
            ArrayList<ModelCategory>filterModels=new ArrayList<>();
            for (int i=0;i<filterList.size();i++){
                //validate
                if (filterList.get(i).getCategory().toUpperCase().contains(constraint)){
                    //add to filterd list
                    filterModels.add(filterList.get(i));
                }
            }
            results.count=filterModels.size();
            results.values=filterModels;
        }
        else {
            results.count= filterList.size();
            results.values=filterList;
        }
        return results;//dont miss it
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
      //apply filter changes
        adapterCategory.categoryArrayList=(ArrayList<ModelCategory>)results.values;

        //notify chnages
        adapterCategory.notifyDataSetChanged();
    }
}
