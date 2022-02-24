package example;
import java.util.Date;

/**
 * Example for EI_EXPOSE_REP bugs
 */
public class MyDate {
    private Date date = null;


    public Date getDate() {
        return date; //a flaw should occur at this point
    }

    public void setDate(Date date) {
        this.date = date; //a flaw should occur at this point
    }

    public Date foo() {
        return new Date(date.getTime());
    }
}
