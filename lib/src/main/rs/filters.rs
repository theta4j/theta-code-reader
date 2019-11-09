#pragma version(1)
#pragma rs java_package_name(org.theta4j.codereader)

rs_allocation allocIn;

uchar __attribute__((kernel)) correctDistortion(uchar in, uint32_t x, uint32_t y)
{
    int width = rsAllocationGetDimX(allocIn) / 2;
    int height = rsAllocationGetDimY(allocIn);

    int offset_x = width / 2 + (x < width ? 0 : width);
    int offset_y = height / 2;

    float x1 = (int)x - offset_x;
    float y1 = (int)y - offset_y;

    float f = 600.0;
    float r1 = hypot(x1, y1);
    float r2 = f * atan(r1 / f);
    float alpha = r2 / r1;

    float x2 = alpha * x1;
    float y2 = alpha * y1;

    uint32_t new_x = clamp((int)x2, -width/2, width/2) + offset_x;
    uint32_t new_y = clamp((int)y2, -height/2, height/2) + offset_y;

    return rsGetElementAt_uchar(allocIn, new_x, new_y);
}

uchar4 __attribute__((kernel)) correctDistortionRGBA(uchar in, uint32_t x, uint32_t y)
{
    uchar v = correctDistortion(in, x, y);
    uchar4 ret = {v, v, v, 255};
    return ret;
}
