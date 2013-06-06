import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.resolver.StandardDialectResolver;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;


import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

class DynomiteUtils {

	public static Dialect resolveDialect(DataSource dataSource) {
	    try {
				return (Dialect) JdbcUtils.extractDatabaseMetaData(dataSource, new DatabaseMetaDataCallback() {
                    @Override
                    public Object processMetaData(DatabaseMetaData dbmd) throws SQLException,
                            MetaDataAccessException {
                        return (new StandardDialectResolver()).resolveDialect(dbmd);
                    }
                });
		} catch (MetaDataAccessException e) {
			System.out.println("Failed to resolve Dialect for DataSource " + dataSource + " no validation query will be resolved"+e.getMessage());
			return null;
		}
	}

}