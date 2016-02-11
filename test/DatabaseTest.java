import org.junit.Test;
import org.springframework.util.Assert;
import play.db.DB;

import javax.sql.DataSource;
import java.sql.Connection;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

public class DatabaseTest {

    @Test
    public void testGetDataSource() {
        running(fakeApplication(), new Runnable() {
            @Override
            public void run() {
                DataSource ds = DB.getDataSource();
                Assert.notNull(ds, "DataSource was null!");
            }
        });
    }

    @Test
    public void testGetConnection() {
        running(fakeApplication(), new Runnable() {
            @Override
            public void run() {
                Connection conn = DB.getConnection();
                Assert.notNull(conn, "Connection was null!");
            }
        });
    }
}
