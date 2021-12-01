import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.smartsheet.api.Smartsheet;
import com.smartsheet.api.SmartsheetFactory;
import com.smartsheet.api.models.MultiRowEmail;
import com.smartsheet.api.models.Recipient;
import com.smartsheet.api.models.RecipientEmail;

public class sendEmail 
{
	//construct an email to be sent to whoever needs to be reminded in the user's Smartsheet
	public static boolean reminder(Timekeeper time) 
	{
		//create a new RecipientEmail object out of who the reminder should be sent to
		RecipientEmail recipientEmail = new RecipientEmail().setEmail(time.assignedTo);

		//create a new list based on who the reminder should be sent to in order to 
		//align with Smartsheet api standards when creating the MultiRowEmail object
		List<Recipient> recipientList = Arrays.asList(recipientEmail);
		
		//grab the row ids so that they can be sent in the email
		List<Long> ids = new ArrayList<Long>();
		ids.add(time.rowId);

		//create a new MultiRowEmail object that will be sent to all recipients who need to be reminded
		//the email will contain the relevant rows that correspond to what the recipient needs to do
		MultiRowEmail multiRowEmail = new MultiRowEmail.AddMultiRowEmailBuilder()
				  .setSendTo(recipientList)
				  .setSubject("Reminder")
				  .setMessage("You need to work on this task")
				  .setCcMe(false)
				  .setRowIds(ids)
				  .setIncludeAttachments(false)
				  .setIncludeDiscussions(false)
				  .build();

		//create a client out of the user's Smartsheet token to send emails
		Smartsheet smartsheet = SmartsheetFactory.createDefaultClient(time.userToken);
		try {
			//send a reminder email with the rows in the sheet to the correct user
			smartsheet.sheetResources().rowResources().sendRows(time.sheetId, multiRowEmail);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
}