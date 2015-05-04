/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hslf.usermodel;

import java.awt.Graphics2D;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.apache.poi.hslf.blip.BitmapPainter;
import org.apache.poi.hslf.blip.DIB;
import org.apache.poi.hslf.blip.EMF;
import org.apache.poi.hslf.blip.ImagePainter;
import org.apache.poi.hslf.blip.JPEG;
import org.apache.poi.hslf.blip.PICT;
import org.apache.poi.hslf.blip.PNG;
import org.apache.poi.hslf.blip.WMF;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * A class that represents image data contained in a slide show.
 *
 *  @author Yegor Kozlov
 */
public abstract class HSLFPictureData {

    protected POILogger logger = POILogFactory.getLogger(this.getClass());

    /**
     * Size of the image checksum calculated using MD5 algorithm.
     */
    protected static final int CHECKSUM_SIZE = 16;

    /**
    * Binary data of the picture
    */
    private byte[] rawdata;
    /**
     * The offset to the picture in the stream
     */
    protected int offset;

    /**
     * Returns type of this picture.
     * Must be one of the static constants defined in the <code>Picture<code> class.
     *
     * @return type of this picture.
     */
    public abstract int getType();


    /**
     * Returns content type (mime type) of this picture.
     *
     * @return content type of this picture.
     */
    public abstract String getContentType();
    
    /**
     * Returns the binary data of this Picture
     * @return picture data
     */
    public abstract byte[] getData();

    /**
     *  Set picture data
     */
    public abstract void setData(byte[] data) throws IOException;

    /**
     * Blip signature.
     */
    protected abstract int getSignature();

    protected static final ImagePainter[] painters = new ImagePainter[8];
    static {
        HSLFPictureData.setImagePainter(HSLFPictureShape.PNG, new BitmapPainter());
        HSLFPictureData.setImagePainter(HSLFPictureShape.JPEG, new BitmapPainter());
        HSLFPictureData.setImagePainter(HSLFPictureShape.DIB, new BitmapPainter());
    }

    /**
     * Returns the raw binary data of this Picture excluding the first 8 bytes
     * which hold image signature and size of the image data.
     *
     * @return picture data
     */
    public byte[] getRawData(){
        return rawdata;
    }

    public void setRawData(byte[] data){
        rawdata = data;
    }

    /**
     * File offset in the 'Pictures' stream
     *
     * @return offset in the 'Pictures' stream
     */
    public int getOffset(){
        return offset;
    }

    /**
     * Set offset of this picture in the 'Pictures' stream.
     * We need to set it when a new picture is created.
     *
     * @param offset in the 'Pictures' stream
     */
    public void setOffset(int offset){
        this.offset = offset;
    }

    /**
     * Returns 16-byte checksum of this picture
     */
    public byte[] getUID(){
        byte[] uid = new byte[16];
        System.arraycopy(rawdata, 0, uid, 0, uid.length);
        return uid;
    }


    /**
     * Compute 16-byte checksum of this picture using MD5 algorithm.
     */
    public static byte[] getChecksum(byte[] data) {
        MessageDigest md5 = CryptoFunctions.getMessageDigest(HashAlgorithm.md5);
        md5.update(data);
        return md5.digest();
    }

    /**
     * Write this picture into <code>OutputStream</code>
     */
    public void write(OutputStream out) throws IOException {
        byte[] data;

        data = new byte[LittleEndian.SHORT_SIZE];
        LittleEndian.putUShort(data, 0, getSignature());
        out.write(data);

        data = new byte[LittleEndian.SHORT_SIZE];
        LittleEndian.putUShort(data, 0, getType() + 0xF018);
        out.write(data);

        byte[] rawdata = getRawData();

        data = new byte[LittleEndian.INT_SIZE];
        LittleEndian.putInt(data, 0, rawdata.length);
        out.write(data);

        out.write(rawdata);
    }

    /**
     * Create an instance of <code>PictureData</code> by type.
     *
     * @param type type of the picture data.
     * Must be one of the static constants defined in the <code>Picture<code> class.
     * @return concrete instance of <code>PictureData</code>
     */
     public static HSLFPictureData create(int type){
        HSLFPictureData pict;
        switch (type){
            case HSLFPictureShape.EMF:
                pict = new EMF();
                break;
            case HSLFPictureShape.WMF:
                pict = new WMF();
                break;
            case HSLFPictureShape.PICT:
                pict = new PICT();
                break;
            case HSLFPictureShape.JPEG:
                pict = new JPEG();
                break;
            case HSLFPictureShape.PNG:
                pict = new PNG();
                break;
            case HSLFPictureShape.DIB:
                pict = new DIB();
                break;
            default:
                throw new IllegalArgumentException("Unsupported picture type: " + type);
        }
        return pict;
    }

    /**
     * Return 24 byte header which preceeds the actual picture data.
     * <p>
     * The header consists of 2-byte signature, 2-byte type,
     * 4-byte image size and 16-byte checksum of the image data.
     * </p>
     *
     * @return the 24 byte header which preceeds the actual picture data.
     */
    public byte[] getHeader() {
        byte[] header = new byte[16 + 8];
        LittleEndian.putInt(header, 0, getSignature());
        LittleEndian.putInt(header, 4, getRawData().length);
        System.arraycopy(rawdata, 0, header, 8, 16);
        return header;
    }

    /**
    * Return image size in bytes
    *
    *  @return the size of the picture in bytes
     * @deprecated Use <code>getData().length</code> instead.
    */
    public int getSize(){
        return getData().length;
    }

    public void draw(Graphics2D graphics, HSLFPictureShape parent){
        ImagePainter painter = painters[getType()];
        if(painter != null) painter.paint(graphics, this, parent);
        else logger.log(POILogger.WARN, "Rendering is not supported: " + getClass().getName());
    }

    /**
     * Register ImagePainter for the specified image type
     *
     * @param type  image type, must be one of the static constants defined in the <code>Picture<code> class.
     * @param painter
     */
    public static void setImagePainter(int type, ImagePainter painter){
        painters[type] = painter;
    }

    /**
     * Return ImagePainter for the specified image type
     *
     * @param type blip type, must be one of the static constants defined in the <code>Picture<code> class.
     * @return ImagePainter for the specified image type
     */
    public static ImagePainter getImagePainter(int type){
        return painters[type];
    }

}