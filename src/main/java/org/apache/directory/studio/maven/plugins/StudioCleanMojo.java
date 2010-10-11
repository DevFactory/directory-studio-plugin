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

import org.apache.maven.plugin.MojoExecutionException;


/**
 * Clean stuff generated by studio:eclipse
 *
 * @goal clean
 * @execute phase="generate-resources"
 * @requiresProject
 * @since 1.0
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StudioCleanMojo extends AbstractStudioMojo
{

    private final String MAVEN_ECLIPSE_XML = "maven-eclipse.xml";
    private final String EXTERNAL_TOOL_BUILDERS_DIR = ".externalToolBuilders";
    private final String META_INF = "META-INF";


    public void execute() throws MojoExecutionException
    {

        if ( project.isExecutionRoot() )
        {
            try
            {
                forkMvnGoal( "eclipse:clean", getActiveProfileIds(), getInactiveProfileIds() );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( e.getMessage() );
            }
        }
        try
        {
            File file = new File( project.getBasedir(), MAVEN_ECLIPSE_XML );
            getLog().info( "Deleting " + file );
            file.delete();
            file = new File( project.getBasedir(), EXTERNAL_TOOL_BUILDERS_DIR );
            getLog().info( "Deleting " + file );
            deleteDirectory( file );
            file = new File( project.getBasedir(), libraryPath );
            getLog().info( "Deleting " + file );
            deleteDirectory( file );
            file = new File( project.getBasedir(), META_INF );
            getLog().info( "Deleting " + file );
            deleteDirectory( file );
        }
        catch ( Exception e )
        {
            getLog().error( e );
        }
    }
}
