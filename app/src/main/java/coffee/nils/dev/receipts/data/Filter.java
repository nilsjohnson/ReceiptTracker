package coffee.nils.dev.receipts.data;

import java.util.ArrayList;
import java.util.Date;

/**
 * This class describes the list of receipts DAO should return.
 */
public class Filter
{
    public Date startDate;
    public Date endDate;
    public ArrayList<String> chosenCategoryList;

    public FilterCase getFilterCase()
    {
        if (startDate == null && endDate == null && chosenCategoryList != null)
        {
            return FilterCase.BY_CATEGORY_ONLY;
        }
        if (startDate != null && endDate != null && chosenCategoryList == null)
        {
            return FilterCase.DATE_ONLY;
        }
        if (startDate != null && endDate != null && chosenCategoryList != null)
        {
            return FilterCase.BY_DATE_AND_CATEGORY;
        }

        return FilterCase.NO_FILTER;
    }

    public void reset()
    {
        startDate = null;
        endDate = null;
        chosenCategoryList = null;
    }
}
