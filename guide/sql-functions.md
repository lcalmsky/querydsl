![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

`SQL` 함수는 `Dialect`로 등록한 언어에 대해서만 사용할 수 있습니다.

현재 프로젝트에서는 `H2` 데이터베이스를 사용하므로 `H2Dialect`에 명시된 함수들만 사용할 수 있습니다.

<details><summary>H2Dialect.java</summary>

```java
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.AvgWithArgumentCastFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.hint.IndexQueryHintHandler;
import org.hibernate.dialect.identity.H2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorH2DatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;

import org.jboss.logging.Logger;

/**
 * A dialect compatible with the H2 database.
 *
 * @author Thomas Mueller
 */
public class H2Dialect extends Dialect {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			H2Dialect.class.getName()
	);

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean bindLimitParametersInReverseOrder() {
			return true;
		}
	};

	private final String querySequenceString;
	private final SequenceInformationExtractor sequenceInformationExtractor;

	/**
	 * Constructs a H2Dialect
	 */
	public H2Dialect() {
		super();

		int buildId = Integer.MIN_VALUE;

		try {
			// HHH-2300
			final Class h2ConstantsClass = ReflectHelper.classForName( "org.h2.engine.Constants" );
			final int majorVersion = (Integer) h2ConstantsClass.getDeclaredField( "VERSION_MAJOR" ).get( null );
			final int minorVersion = (Integer) h2ConstantsClass.getDeclaredField( "VERSION_MINOR" ).get( null );
			buildId = (Integer) h2ConstantsClass.getDeclaredField( "BUILD_ID" ).get( null );

			if ( ! ( majorVersion > 1 || minorVersion > 2 || buildId >= 139 ) ) {
				LOG.unsupportedMultiTableBulkHqlJpaql( majorVersion, minorVersion, buildId );
			}
		}
		catch ( Exception e ) {
			// probably H2 not in the classpath, though in certain app server environments it might just mean we are
			// not using the correct classloader
			LOG.undeterminedH2Version();
		}

		if ( buildId >= 32 ) {
			this.sequenceInformationExtractor = buildId >= 201
					? SequenceInformationExtractorLegacyImpl.INSTANCE
					: SequenceInformationExtractorH2DatabaseImpl.INSTANCE;
			this.querySequenceString = "select * from INFORMATION_SCHEMA.SEQUENCES";
		}
		else {
			this.sequenceInformationExtractor = SequenceInformationExtractorNoOpImpl.INSTANCE;
			this.querySequenceString = null;
		}

		registerColumnType( Types.BOOLEAN, "boolean" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BINARY, "binary" );
		registerColumnType( Types.BIT, "boolean" );
		registerColumnType( Types.CHAR, "char($l)" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, buildId >= 201 ? "numeric($p,$s)" : "decimal($p,$s)" );
		registerColumnType( Types.NUMERIC, buildId >= 201 ? "numeric($p,$s)" : "decimal($p,$s)" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.LONGVARBINARY, "longvarbinary" );
		// H2 does define "longvarchar", but it is a simple alias to "varchar"
		registerColumnType( Types.LONGVARCHAR, String.format( "varchar(%d)", Integer.MAX_VALUE ) );
		registerColumnType( Types.REAL, "real" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.VARBINARY, buildId >= 201 ? "varbinary($l)" : "binary($l)" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		// Aggregations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "avg", new AvgWithArgumentCastFunction( "double" ) );

		// select topic, syntax from information_schema.help
		// where section like 'Function%' order by section, topic
		//
		// see also ->  http://www.h2database.com/html/functions.html

		// Numeric Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "acos", new StandardSQLFunction( "acos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new StandardSQLFunction( "atan2", StandardBasicTypes.DOUBLE ) );
		registerFunction( "bitand", new StandardSQLFunction( "bitand", StandardBasicTypes.INTEGER ) );
		registerFunction( "bitor", new StandardSQLFunction( "bitor", StandardBasicTypes.INTEGER ) );
		registerFunction( "bitxor", new StandardSQLFunction( "bitxor", StandardBasicTypes.INTEGER ) );
		registerFunction( "ceiling", new StandardSQLFunction( "ceiling", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "compress", new StandardSQLFunction( "compress", StandardBasicTypes.BINARY ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", StandardBasicTypes.DOUBLE ) );
		registerFunction( "decrypt", new StandardSQLFunction( "decrypt", StandardBasicTypes.BINARY ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardBasicTypes.DOUBLE ) );
		registerFunction( "encrypt", new StandardSQLFunction( "encrypt", StandardBasicTypes.BINARY ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "expand", new StandardSQLFunction( "compress", StandardBasicTypes.BINARY ) );
		registerFunction( "floor", new StandardSQLFunction( "floor", StandardBasicTypes.DOUBLE ) );
		registerFunction( "hash", new StandardSQLFunction( "hash", StandardBasicTypes.BINARY ) );
		registerFunction( "log", new StandardSQLFunction( "log", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", StandardBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", StandardBasicTypes.DOUBLE ) );
		registerFunction( "power", new StandardSQLFunction( "power", StandardBasicTypes.DOUBLE ) );
		registerFunction( "radians", new StandardSQLFunction( "radians", StandardBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgSQLFunction( "rand", StandardBasicTypes.DOUBLE ) );
		registerFunction( "round", new StandardSQLFunction( "round", StandardBasicTypes.DOUBLE ) );
		registerFunction( "roundmagic", new StandardSQLFunction( "roundmagic", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "truncate", new StandardSQLFunction( "truncate", StandardBasicTypes.DOUBLE ) );

		// String Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardBasicTypes.INTEGER ) );
		registerFunction( "char", new StandardSQLFunction( "char", StandardBasicTypes.CHARACTER ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "difference", new StandardSQLFunction( "difference", StandardBasicTypes.INTEGER ) );
		registerFunction( "hextoraw", new StandardSQLFunction( "hextoraw", StandardBasicTypes.STRING ) );
		registerFunction( "insert", new StandardSQLFunction( "lower", StandardBasicTypes.STRING ) );
		registerFunction( "left", new StandardSQLFunction( "left", StandardBasicTypes.STRING ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase", StandardBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim", StandardBasicTypes.STRING ) );
		registerFunction( "octet_length", new StandardSQLFunction( "octet_length", StandardBasicTypes.INTEGER ) );
		registerFunction( "position", new StandardSQLFunction( "position", StandardBasicTypes.INTEGER ) );
		registerFunction( "rawtohex", new StandardSQLFunction( "rawtohex", StandardBasicTypes.STRING ) );
		registerFunction( "repeat", new StandardSQLFunction( "repeat", StandardBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
		registerFunction( "right", new StandardSQLFunction( "right", StandardBasicTypes.STRING ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", StandardBasicTypes.STRING ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardBasicTypes.STRING ) );
		registerFunction( "space", new StandardSQLFunction( "space", StandardBasicTypes.STRING ) );
		registerFunction( "stringencode", new StandardSQLFunction( "stringencode", StandardBasicTypes.STRING ) );
		registerFunction( "stringdecode", new StandardSQLFunction( "stringdecode", StandardBasicTypes.STRING ) );
		registerFunction( "stringtoutf8", new StandardSQLFunction( "stringtoutf8", StandardBasicTypes.BINARY ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase", StandardBasicTypes.STRING ) );
		registerFunction( "utf8tostring", new StandardSQLFunction( "utf8tostring", StandardBasicTypes.STRING ) );

		// Time and Date Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "curdate", new NoArgSQLFunction( "curdate", StandardBasicTypes.DATE ) );
		registerFunction( "curtime", new NoArgSQLFunction( "curtime", StandardBasicTypes.TIME ) );
		registerFunction( "curtimestamp", new NoArgSQLFunction( "curtimestamp", StandardBasicTypes.TIME ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "datediff", new StandardSQLFunction( "datediff", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardBasicTypes.STRING ) );
		registerFunction( "now", new NoArgSQLFunction( "now", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardBasicTypes.INTEGER ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardBasicTypes.INTEGER ) );

		// System Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "database", new NoArgSQLFunction( "database", StandardBasicTypes.STRING ) );
		registerFunction( "user", new NoArgSQLFunction( "user", StandardBasicTypes.STRING ) );

		getDefaultProperties().setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		// http://code.google.com/p/h2database/issues/detail?id=235
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {
		return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "call next value for " + sequenceName;
	}

	@Override
	public String getQuerySequencesString() {
		return querySequenceString;
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return sequenceInformationExtractor;
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			String constraintName = null;
			// 23000: Check constraint violation: {0}
			// 23001: Unique index or primary key violation: {0}
			if ( sqle.getSQLState().startsWith( "23" ) ) {
				final String message = sqle.getMessage();
				final int idx = message.indexOf( "violation: " );
				if ( idx > 0 ) {
					constraintName = message.substring( idx + "violation: ".length() );
				}
				if ( sqle.getSQLState().equals("23506") ) {
					constraintName  = constraintName.substring( 1, constraintName.indexOf(":") );
				}
			}
			return constraintName;
		}
	};

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		SQLExceptionConversionDelegate delegate = super.buildSQLExceptionConversionDelegate();
		if (delegate == null) {
			delegate = new SQLExceptionConversionDelegate() {
				@Override
				public JDBCException convert(SQLException sqlException, String message, String sql) {
					final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

					if (40001 == errorCode) {
						// DEADLOCK DETECTED
						return new LockAcquisitionException(message, sqlException, sql);
					}

					if (50200 == errorCode) {
						// LOCK NOT AVAILABLE
						return new PessimisticLockException(message, sqlException, sql);
					}

					if ( 90006 == errorCode ) {
						// NULL not allowed for column [90006-145]
						final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName( sqlException );
						return new ConstraintViolationException( message, sqlException, sql, constraintName );
					}

					return null;
				}
			};
		}
		return delegate;
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new LocalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String getCreateIdTableCommand() {
						return "create cached local temporary table if not exists";
					}

					@Override
					public String getCreateIdTableStatementOptions() {
						// actually 2 different options are specified here:
						//		1) [on commit drop] - says to drop the table on transaction commit
						//		2) [transactional] - says to not perform an implicit commit of any current transaction
						return "on commit drop transactional";					}
				},
				AfterUseAction.CLEAN,
				TempTableDdlTransactionHandling.NONE
		);
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "call current_timestamp()";
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return true;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// see http://groups.google.com/group/h2-database/browse_thread/thread/562d8a49e2dabe99?hl=en
		return true;
	}

	@Override
	public boolean supportsTuplesInSubqueries() {
		return false;
	}

	// Do not drop constraints explicitly, just do this by cascading instead.
	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " CASCADE ";
	}

	// CASCADE has to be AFTER IF EXISTS in case it's after the tablename
	@Override
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new H2IdentityColumnSupport();
	}

	@Override
	public String getQueryHintString(String query, String hints) {
		return IndexQueryHintHandler.INSTANCE.addQueryHints( query, hints );
	}

	@Override
	public boolean supportsSelectAliasInGroupByClause() {
		return true;
	}
}
```

</details>

먼저 `Replace` 함수를 사용해보겠습니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PlayerTest {
    @Autowired
    EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    @BeforeEach
    void setup() {
        Team tottenhamHotspur = new Team("Tottenham Hotspur F.C.");
        Team manchesterCity = new Team("Manchester City F.C.");
        entityManager.persist(tottenhamHotspur);
        entityManager.persist(manchesterCity);

        Player harryKane = new Player("Harry Kane", 27, tottenhamHotspur);
        harryKane.contactSalary(200000);
        harryKane.begins();
        Player heungminSon = new Player("Heungmin Son", 29, tottenhamHotspur);
        heungminSon.contactSalary(140000);
        heungminSon.begins();
        Player kevinDeBruyne = new Player("Kevin De Bruyne", 30, manchesterCity);
        kevinDeBruyne.contactSalary(350000);
        kevinDeBruyne.begins();
        Player raheemSterling = new Player("Raheem Shaquille Sterling", 26, manchesterCity);
        raheemSterling.contactSalary(300000);
        raheemSterling.begins();

        entityManager.persist(harryKane);
        entityManager.persist(heungminSon);
        entityManager.persist(kevinDeBruyne);
        entityManager.persist(raheemSterling);
        queryFactory = new JPAQueryFactory(entityManager);
    }

    @Test
    void simpleQuerydslWithReplaceFunction() {
        // when
        List<String> playerNames = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", player.name, "Son", "S.")) // (1)
                .from(player)
                .fetch();
        // then
        assertTrue(playerNames.stream().anyMatch(name -> name.endsWith("S.")));
        // print
        playerNames.forEach(System.out::println);
    }
}
```

> (1) `Expressions`의 `stringTemplate`을 호출하여 문자열로 직접 `function`을 작성합니다. `function`에 넘겨줄 파라미터는 실제로 사용할 함수와 그 함수의 파라미터를 순차적으로 매핑할 수 있는 값입니다. 각각에 매핑될 파라미터는 함수 문자열을 닫은 뒤 순차적으로 파라미터에 추가합니다. `stringTemplate` 자체는 단순히 `String.format`과 같은 역할을 해주는 메서드이지만 `StringExpression`을 반환하여 `select` 문 안에서 사용할 수 있게 해줍니다. 

테스트를 실행해보면,

```java
2021-07-24 17:58:45.023 DEBUG 18810 --- [           main] org.hibernate.SQL                        : 
    /* select
        function('replace',
        player.name,
        ?1,
        ?2) 
    from
        Player player */ select
            replace(player0_.name,
            ?,
            ?) as col_0_0_ 
        from
            player player0_
Harry Kane
Heungmin S.
Kevin De Bruyne
Raheem Shaquille Sterling
```

이렇게 성공적으로 실행된 것을 확인할 수 있습니다,

> ⚠️ **Warning**: 실행중에 `QuerySyntaxException`가 발생한다면 `function` 이후 작성한 문법에 오류가 있을 수 있습니다. 특히 괄호를 닫지 않았거나 파라미터 갯수가 정확하지 않는 등의 실수를 할 수 있으니 쿼리를 다시 한 번 확인해보시기 바랍니다.

H2에서 제공하는 모든 `function`은 [여기](http://www.h2database.com/html/functions.html)서 확인할 수 있습니다.

표준에 해당하는 함수들은 `Querydsl`에도 내장이 되어있습니다.

`Q Type`에서 제공하기 때문에 훨씬 더 간단히 사용 가능합니다.

선수들의 이름을 조회하여 모두 소문자로 바꾼 뒤 검증하는 테스트를 작성해보겠습니다.

```java
@Test
void simpleQuerydslWithLowerFunction() {
    // when
    List<String> playerNames = queryFactory
            .select(player.name.lower()) // (1)
            .from(player)
            .fetch();
    // then
    assertTrue(playerNames.stream().allMatch(name -> Pattern.matches("[a-z ]+", name))); // (2)
    // print
    playerNames.forEach(System.out::println);
}
```

> (1) Q Type에서 바로 lower()를 호출합니다.  
> (2) 패턴 매칭을 이용해 소문자와 공백만 포함하는지 확인합니다.

```text
2021-07-24 18:37:49.422 DEBUG 19150 --- [           main] org.hibernate.SQL                        : 
    /* select
        lower(player.name) 
    from
        Player player */ select
            lower(player0_.name) as col_0_0_ 
        from
            player player0_
harry kane
heungmin son
kevin de bruyne
raheem shaquille sterling
```

테스트 결과 `lower` 함수를 제대로 호출하였고 결과 또한 모두 소문자로 변경되었음을 확인할 수 있습니다.

---

다음 포스팅부터는 `스프링 데이터 JPA`와 `Querydsl`을 같이 사용하는 방식에 대해 소개하겠습니다.