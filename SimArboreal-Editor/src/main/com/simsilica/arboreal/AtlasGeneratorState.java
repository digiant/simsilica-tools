/*
 * $Id$
 *
 * Copyright (c) 2014, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.arboreal;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.simsilica.arboreal.builder.Builder;
import com.simsilica.arboreal.builder.BuilderReference;
import com.simsilica.arboreal.mesh.BillboardedLeavesMeshGenerator;
import com.simsilica.arboreal.mesh.SkinnedTreeMeshGenerator;
import com.simsilica.arboreal.mesh.Vertex;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.BaseAppState;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 *  @author    Paul Speed
 */
public class AtlasGeneratorState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(AtlasGeneratorState.class);
    
    private VersionedReference<TreeParameters> treeParametersRef;
    private Material treeMaterial;    
    private Material leafMaterial;    

    private Builder builder;
    private AtlasTreeBuilderReference builderRef; 

    private Mesh trunkMesh;
    private Mesh leafMesh;
    
    private TreeView[] views = new TreeView[4];    

    @Override
    protected void initialize( Application app ) {
 
        this.treeParametersRef = getState(TreeParametersState.class).getTreeParametersRef();
        this.treeMaterial = getState(ForestGridState.class).getTreeMaterial();
        this.leafMaterial = getState(ForestGridState.class).getLeafMaterial();
 
        this.builder = getState(BuilderState.class).getBuilder();
        this.builderRef = new AtlasTreeBuilderReference();
        
        
        Camera camera = app.getCamera().clone();   
        camera.resize(512, 512, true);
        camera.resize(1024, 512, false);
  
 
        FrameBuffer fb = new FrameBuffer(1024, 512, 1);
        Texture2D fbTex = new Texture2D(1024, 512, Format.RGBA8);
        fb.setDepthBuffer(Format.Depth);
        fb.setColorTexture(fbTex);
        
        getState(ForestGridState.class).getImpostorMaterial().setTexture("DiffuseMap", fbTex);
        
        
        /*Quad testQuad = new Quad(1024, 512);
        Geometry testGeom = new Geometry("test", testQuad);
        Material mat = GuiGlobals.getInstance().createMaterial(fbTex, false).getMaterial();
        testGeom.setMaterial(mat);
        ((TreeEditor)app).getGuiNode().attachChild(testGeom);*/
        
 
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(new ColorRGBA(1, 1, 1, 1));
        sun.setDirection(new Vector3f(0, 0, -1));
 
        for( int i = 0; i < 4; i++ ) {
            views[i] = new TreeView(fb, camera, sun, 0.25f * i, 0);
        }
    }

    @Override
    protected void cleanup( Application app ) {
        for( TreeView view : views ) {
            app.getRenderManager().removeMainView(view.viewport);
        }
    }

    @Override
    protected void enable() {
    }

    @Override
    protected void disable() {
    }

    protected void updateTree( Mesh trunkMesh, Mesh leafMesh ) {
        if( this.trunkMesh == trunkMesh ) {
            return;
        }
               
        releaseMesh(this.trunkMesh);
        releaseMesh(this.leafMesh);
        this.trunkMesh = trunkMesh;
        this.leafMesh = leafMesh;
        
        for( TreeView view : views ) {
            if( view != null ) {
                view.updateMesh(trunkMesh, leafMesh);
            }
        }
    }

    protected void releaseMesh( Mesh mesh ) {
        if( mesh == null ) {
            return;
        }
 
        // Delete the old buffers
        for( VertexBuffer vb : mesh.getBufferList() ) {
            if( log.isTraceEnabled() ) {
                log.trace("--destroying buffer:" + vb);
            }
            BufferUtils.destroyDirectBuffer( vb.getData() );
        }                            
    }        

    private float nextUpdateCheck = 0.1f;
    private float lastTpf;
    @Override
    public void update( float tpf ) {
        lastTpf = tpf;
        
        nextUpdateCheck += tpf;
        if( nextUpdateCheck <= 0.1f ) {
            return;
        }
        nextUpdateCheck = 0;

        boolean changed = treeParametersRef.update();
        if( changed ) {
            builder.build(builderRef);
        }
        
        /**
         for bounds debugging
        for( TreeView view : views ) {
            view.root.rotate(0, tpf, 0);
        }
        */                
    }

    @Override
    public void render( RenderManager rm ) {
        if( views != null ) {
            // We update the logical state here because it is
            // done after the other updates.  So if another app
            // state or control has modified our root then we
            // are guaranteed to run after.
            for( TreeView view : views ) {
                if( view != null ) {
                    view.root.updateLogicalState(lastTpf);
                    view.root.updateGeometricState();
                }               
            }
        }
    }

    private class TreeView {
        ViewPort viewport;
        Camera camera;
        Node root;
        Geometry trunkGeom;
        Geometry leafGeom;
        Geometry wireBounds;
        boolean debugBounds = false;
        
        public TreeView( FrameBuffer fb, Camera templateCamera, DirectionalLight sun, float x, float y ) {
        
            this.camera = templateCamera.clone();
            camera.resize(512, 512, true);
            camera.resize(1024, 512, false);
            camera.setViewPort(x, x + 0.25f, y, y + 0.5f);
            
            this.root = new Node("Root:" + x + ", " + y);
            this.viewport = getApplication().getRenderManager().createMainView("tree[" + x + ", " + y + "]", camera);
            this.viewport.setOutputFrameBuffer(fb);
            this.root.rotate(0, x * FastMath.TWO_PI + FastMath.QUARTER_PI, 0);
            
            viewport.attachScene(root);
            root.addLight(sun);
            
            viewport.setClearFlags(true, true, true);
            viewport.setBackgroundColor(new ColorRGBA(0, 0, 0, 0));
            this.camera.lookAtDirection(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
        }
        
        public void updateMesh( Mesh trunkMesh, Mesh leafMesh ) {
            if( trunkGeom == null ) {
                // Create it
                trunkGeom = new Geometry("Trunk", trunkMesh);
                trunkGeom.setMaterial(treeMaterial);
                root.attachChild(trunkGeom);
            } else {
                // Just swap out the mesh
                trunkGeom.setMesh(trunkMesh);
            }
            if( leafMesh == null ) {
                if( leafGeom != null ) {
                    leafGeom.removeFromParent();
                    leafGeom = null;
                }
            } else {            
                if( leafGeom == null ) {
                    // Create it
                    leafGeom = new Geometry("Leaves", leafMesh);
                    leafGeom.setMaterial(leafMaterial);
                    leafGeom.setQueueBucket(Bucket.Transparent);  
                    root.attachChild(leafGeom); 
                } else {
                    // Just swap out the mesh
                    leafGeom.setMesh(leafMesh);
                }
            }                
            updateCamera();
        }
        
        protected void updateCamera() {
 
            BoundingBox bb = (BoundingBox)trunkGeom.getModelBound();
            if( leafGeom != null ) {
                BoundingBox bb2 = (BoundingBox)leafGeom.getModelBound();
                bb = (BoundingBox)bb.merge(bb2);
            }
            float size = bb.getYExtent();
    
            // In the projection matrix, [1][1] should be:
            //      (2 * Zn) / camHeight
            // where Zn is distance to near plane.
            float m11 = camera.getViewProjectionMatrix().m11;

            // We want our position to be such that
            // 'size' is otherwise = cameraHeight when rendered.
            float z = m11 * size;
        
            // Add the z extents so that we adjust for the near plane
            // of the bounding box... well we will be rotating so
            // let's just be sure and take the max of x and z
            float offset = Math.max(bb.getXExtent(), bb.getZExtent());
            z += offset;
        
            Vector3f center = bb.getCenter().add(trunkGeom.getLocalTranslation());
        
            camera.setLocation(new Vector3f(0, center.y, z));
 
            if( debugBounds ) {       
                WireBox box;        
                if( wireBounds == null ) {
                    box = new WireBox();
                    wireBounds = new Geometry("wire box", box);
                    Material mat = GuiGlobals.getInstance().createMaterial(ColorRGBA.Yellow, false).getMaterial();
                    wireBounds.setMaterial(mat);
                    root.attachChild(wireBounds);
                } else {
                    box = (WireBox)wireBounds.getMesh();
                }
                box.updatePositions(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
                box.setBound(new BoundingBox(new Vector3f(0,0,0), 0, 0, 0));
                wireBounds.setLocalTranslation(bb.getCenter());
                wireBounds.move(trunkGeom.getLocalTranslation());
                wireBounds.setLocalRotation(trunkGeom.getLocalRotation());
            }        
        }

    }


    private class AtlasTreeBuilderReference implements BuilderReference {

        private Mesh trunkMesh;
        private Mesh leafMesh;        

        @Override
        public int getPriority() {
            // A relatively low priority
            return 100;
        }

        @Override
        public void build() {

            TreeParameters treeParameters = treeParametersRef.get();
            
            TreeGenerator treeGen = new TreeGenerator();        
            Tree tree = treeGen.generateTree(treeParameters);
            
            SkinnedTreeMeshGenerator meshGen = new SkinnedTreeMeshGenerator();
        
            List<Vertex> tips = new ArrayList<Vertex>();
            trunkMesh = meshGen.generateMesh(tree,
                                             treeParameters.getLod(0),
                                             treeParameters.getYOffset(), 
                                             treeParameters.getTextureURepeat(),
                                             treeParameters.getTextureVScale(),
                                             tips);

            if( treeParameters.getGenerateLeaves() ) {
                BillboardedLeavesMeshGenerator leafGen = new BillboardedLeavesMeshGenerator();
                leafMesh = leafGen.generateMesh(tips, treeParameters.getLeafScale());
            } else {
                leafMesh = null;
            }            
        }

        @Override
        public void apply() {
            // Set the new trunk
            updateTree(trunkMesh, leafMesh);               
        }

        @Override
        public void release() {
            
        }        
    }
}