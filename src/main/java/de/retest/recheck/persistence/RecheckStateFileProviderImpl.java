package de.retest.recheck.persistence;

import static de.retest.util.FileUtil.canonicalPathQuietly;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.retest.Properties;
import de.retest.persistence.Persistence;
import de.retest.ui.descriptors.SutState;
import de.retest.util.FileUtil;

public class RecheckStateFileProviderImpl implements RecheckStateFileProvider {

	public static final String RECHECK_PROJECT_ROOT = "de.retest.recheck.root";

	private static final Logger logger = LoggerFactory.getLogger( RecheckStateFileProviderImpl.class );

	private final Persistence<SutState> persistence;

	public RecheckStateFileProviderImpl( final Persistence<SutState> persistence ) {
		this.persistence = persistence;
	}

	@Override
	public File getRecheckStateFile( final String filePath ) throws NoStateFileFoundException {
		final Path projectRootStates = getStates( getProjectRoot(), filePath );
		if ( projectRootStates != null ) {
			return projectRootStates.toFile();
		}

		final Path retestRootStates = getStates( Properties.getReTestInstallDir().toPath(), filePath );
		if ( retestRootStates != null ) {
			return retestRootStates.toFile();
		}

		throw new NoStateFileFoundException( filePath );
	}

	private Path getStates( final Path projectRoot, final String filePath ) {
		if ( projectRoot != null && filePath != null ) {
			logger.debug( "Looking for SUT state files in '{}'.", projectRoot );
			final Path statePath = Paths.get( projectRoot.toAbsolutePath().toString(), filePath );
			if ( Files.exists( statePath, LinkOption.NOFOLLOW_LINKS ) ) {
				return statePath;
			}
		}
		return null;
	}

	@Override
	public SutState loadRecheckState( final File file ) {
		final SutState result;
		try {
			result = persistence.load( file.toURI() );
		} catch ( final IOException e ) {
			throw new UncheckedIOException(
					"Could not load SUT state from file '" + canonicalPathQuietly( file ) + "'.", e );
		}
		if ( result == null ) {
			throw new NullPointerException(
					"Loaded SUT state from file " + canonicalPathQuietly( file ) + "' is null." );
		}
		return result;
	}

	@Override
	public void saveRecheckState( final File file, final SutState state ) {
		try {
			persistence.save( file.toURI(), state );
			logger.info( "Updated SUT state file {}.", FileUtil.canonicalPathQuietly( file ) );
		} catch ( final IOException e ) {
			throw new UncheckedIOException(
					"Could not apply changes to SUT state file '" + canonicalPathQuietly( file ) + "'.", e );
		}
	}

	private Path getProjectRoot() {
		final String projectRoot = System.getProperty( RECHECK_PROJECT_ROOT );
		if ( projectRoot != null ) {
			return Paths.get( projectRoot );
		}
		return null;
	}

}
