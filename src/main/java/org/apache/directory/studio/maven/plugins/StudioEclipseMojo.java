/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.studio.maven.plugins;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;


/**
 * Prepares for eclipse:
 * <p>
 * <ul>
 * <li>Fork eclipse:eclipse goal and adapt generate files where needed</li>
 * <li>Copy artifacts nonscoped "provided" to ${basedir}/lib</li>
 * <li>Adapt ${basedir}/.classpath for artifacts nonscoped "provided"</li>
 * </ul>
 * </p>
 * 
 * @goal eclipse
 * @execute phase="generate-resources"
 * @description Copy artifacts nonscoped "provided" to libraryPath, Add
 *              artifacts nonscoped "provided" to Bundle-ClassPath and
 *              MANIFEST.MF, Adapt ${basedir}/.classpath for artifacts nonscoped
 *              "provided"
 * @requiresProject
 * @requiresDependencyResolution runtime
 * @since 1.0
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StudioEclipseMojo extends AbstractStudioMojo
{

    /**
     * Bundle-ClassPath: updated with the list of dependencies.
     */
    public final static String ENTRY_BUNDLE_CLASSPATH = "Bundle-ClassPath:";

    /**
     * Bundle symbolic name: updated with the artifact id.
     */
    public final static String ENTRY_BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName:";

    /**
     * Bundle version: updated with the project version.
     */
    public final static String ENTRY_BUNDLE_VERSION = "Bundle-Version:";


    public void execute() throws MojoExecutionException
    {
        if ( project.isExecutionRoot() )
        {
            try
            {
                forkMvnGoal( "eclipse:eclipse" );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( e.getMessage() );
            }
        }
        if ( !skip )
        {
            try
            {
                // Create list of used artifacts
                final List<Artifact> artifactList = createArtifactList();

                // copy Artifacts
                copyArtifacts( artifactList );

                try
                {
                    forkMvnGoal( "bundle:manifest" );
                }
                catch ( Exception e )
                {
                    throw new MojoExecutionException( e.getMessage() );
                }

                // Update .classpath
                updateDotClasspath( artifactList );

                updateDotProject();
                removeMavenEclipseXml();
                removeDotExternalToolBuilders();

            }
            catch ( FileNotFoundException e )
            {
                getLog().error(
                    "Please run eclipse:eclipse first to create .classpath, e.g. mvn eclipse:eclipse studio:eclipse.",
                    e );
            }
            catch ( Exception e )
            {
                getLog().error( e );
            }
        }
    }


    /**
     * Copy artifacts to ${basedir}/lib
     * 
     * @param list
     * @throws IOException
     */
    private void copyArtifacts( final List<Artifact> list ) throws IOException
    {
        // Only proceed when we have artifacts to process
        if ( !list.isEmpty() )
        {
            final File copyDir = new File( project.getBasedir(), libraryPath );

            if ( !copyDir.exists() )
                copyDir.mkdirs();

            for ( Artifact artifact : list )
            {
                if ( !artifact.getScope().equalsIgnoreCase( "test" ) )
                {
                    final File destFile = new File( copyDir, artifact.getFile().getName() );
                    FileUtils.copyFile( artifact.getFile(), destFile );
                    getLog().info( "Copying " + artifact.getFile() + " to " + destFile );
                }
            }
        }
    }


    /**
     * Adapt the ${basedir}/.classpath
     * 
     * @param list
     */
    private void updateDotClasspath( List<Artifact> list ) throws IOException, XmlPullParserException
    {
        getLog().info( "Update .classpath in " + project.getBasedir() );
        final InputStream is = new FileInputStream( new File( project.getBasedir(), ".classpath" ) );
        Xpp3Dom dom = Xpp3DomBuilder.build( is, "UTF-8" );
        int cnt = 0;
        for ( Xpp3Dom cpEntry : dom.getChildren( "classpathentry" ) )
        {
            if ( cpEntry.getAttribute( "kind" ).equals( "lib" ) )
            {
                dom.removeChild( cnt );
                cnt--;
            }
            cnt++;
        }

        Xpp3Dom entry;
        for ( Artifact artifact : list )
        {
            if ( artifact.getScope().equalsIgnoreCase( "test" ) )
            {
                entry = new Xpp3Dom( "classpathentry" );
                entry.setAttribute( "kind", "lib" );
                entry.setAttribute( "path", ( new StringBuilder() ).append( libraryPath ).append( '/' ).append(
                    artifact.getFile().getName() ).toString() );
            }
            else
            {
                entry = new Xpp3Dom( "classpathentry" );
                entry.setAttribute( "exported", "true" );
                entry.setAttribute( "kind", "lib" );
                entry.setAttribute( "path", ( new StringBuilder() ).append( libraryPath ).append( '/' ).append(
                    artifact.getFile().getName() ).toString() );
            }
            dom.addChild( entry );
        }

        is.close();
        Writer w = new OutputStreamWriter( new FileOutputStream( new File( project.getBasedir(), ".classpath" ) ),
            "UTF-8" );
        org.codehaus.plexus.util.xml.XMLWriter writer = new PrettyPrintXMLWriter( w );
        Xpp3DomWriter.write( writer, dom );
        w.flush();
        w.close();
    }


    /**
     * Adapt the ${basedir}/.project
     * 
     * @param list
     */
    private void updateDotProject() throws IOException, XmlPullParserException
    {
        getLog().info( "Update .project in " + project.getBasedir() );
        InputStream is = new FileInputStream( new File( project.getBasedir(), ".project" ) );
        Xpp3Dom dom = Xpp3DomBuilder.build( is, "UTF-8" );
        int cnt = 0;
        for ( Xpp3Dom cpEntry : dom.getChild( "buildSpec" ).getChildren( "buildCommand" ) )
        {
            if ( cpEntry.getChild( "name" ).getValue().equals( "org.eclipse.ui.externaltools.ExternalToolBuilder" ) )
            {
                dom.getChild( "buildSpec" ).removeChild( cnt );
                cnt--;
            }
            cnt++;
        }

        removeChildFromDom( dom, "linkedResources" );
        removeChildFromDom( dom, "projects" );

        is.close();
        Writer w = new OutputStreamWriter( new FileOutputStream( new File( project.getBasedir(), ".project" ) ),
            "UTF-8" );
        org.codehaus.plexus.util.xml.XMLWriter writer = new PrettyPrintXMLWriter( w );
        Xpp3DomWriter.write( writer, dom );
        w.flush();
        w.close();
    }


    /**
     * remove ${basedir}/maven-eclipse.xml
     */
    void removeMavenEclipseXml()
    {
        File file = new File( project.getBasedir(), "maven-eclipse.xml" );
        if ( file.exists() )
            file.delete();
    }


    /**
     * Adapt the ${basedir}/.externalToolBuilders
     */
    void removeDotExternalToolBuilders()
    {
        File file = new File( project.getBasedir(), ".externalToolBuilders" );
        if ( file.exists() )
            deleteDirectory( file );
    }


    private void removeChildFromDom( Xpp3Dom dom, String childName )
    {
        int cnt = 0;
        for ( Xpp3Dom child : dom.getChildren() )
        {
            if ( child.getName().equals( childName ) )
            {
                dom.removeChild( cnt );
                cnt -= 1;
            }
            cnt += 1;
        }
    }
}
