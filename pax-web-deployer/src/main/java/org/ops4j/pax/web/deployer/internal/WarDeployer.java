package org.ops4j.pax.web.deployer.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;

/**
 * An Apache Felix FileInstall transform for WAR files.
 *
 * @author Alin Dreghiciu, Achim Nierbeck
 */
public class WarDeployer
    implements ArtifactUrlTransformer
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( WarDeployer.class );

    /**
     * Standard PATH separator 
     */
	private static final String PATH_SEPERATOR = "/";


    public boolean canHandle( final File artifact )
    {
    	try {
			JarFile jar = new JarFile(artifact);
			JarEntry entry = jar.getJarEntry("WEB-INF/web.xml");
			// Only handle WAR artifacts
			if (entry == null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("No war file do not handle artifact:"+artifact.getName());
				}
				return false;
			}
			// Only handle non OSGi bundles
			Manifest m = jar.getManifest();
			if (m!= null && m.getMainAttributes().getValue(
					new Attributes.Name("Bundle-SymbolicName")) != null
					&& m.getMainAttributes().getValue(
							new Attributes.Name("Bundle-Version")) != null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("This artifact has OSGi Manifest Header skipping: "+artifact.getName());
				}
				return false;
			}
		} catch (Exception e) {
			if (LOG.isDebugEnabled())
				LOG.debug("Can't handle file "+artifact.getName(), e);
			return false;
		}

        try
        {
            new URL( "webbundle", null, artifact.toURL().toExternalForm() );
        }
        catch( MalformedURLException e )
        {
            LOG.warn(
                String.format(
                    "File %s could not be transformed. Most probably that Pax URL WAR handler is not installed",
                    artifact.getAbsolutePath()
                )
            );
            return false;
        }

        return true;
    }

    public URL transform( final URL artifact )
        throws Exception
    {
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("Transforming artifact with URL: "+artifact);
    	}
        final String path = artifact.getPath();
        final String protocol = artifact.getProtocol();
        if( path != null )
        {
        	int idx = -1;
        	// match the last slash to retrieve the name of the archive
    		if ("jardir".equalsIgnoreCase(protocol)) {
    		    // just to make sure this works on all kinds of windows
    		    File fileInstance = new File(path);
    		    // with a jardir this is system specific
    		    idx = fileInstance.getAbsolutePath().lastIndexOf(File.separator);
    		} else {
    		    // a standard file is not system specific, this is always a standardized URL path
    			idx = path.lastIndexOf(PATH_SEPERATOR);
    		}
    		// match the suffix so we get rid of it for displaying
            if( idx > 0 )
            {
                final String[] name = path.substring( idx + 1 ).split( "\\." );
                final StringBuilder url = new StringBuilder();
                url.append( artifact.toExternalForm() );
                if( artifact.toExternalForm().contains( "?" ) )
                {
                    url.append( "&" );
                }
                else
                {
                    url.append( "?" );
                }
                url.append( "Web-ContextPath=" ).append( name[0] );
                url.append( "&" );
                url.append( "Bundle-SymbolicName=" ).append( name[0] );

                if (LOG.isDebugEnabled()) {
                	LOG.debug(String.format("Transformed URL of %s to following %s", path, url));
                }
                return new URL( "webbundle", null, url.toString() );
            }
        }
        if (LOG.isDebugEnabled()) {
        	LOG.debug("No path for given artifact, retry with webbundle prepended");
        }
        return new URL( "webbundle", null, artifact.toExternalForm() );
    }

}
