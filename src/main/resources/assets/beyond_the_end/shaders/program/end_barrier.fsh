#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 OutSize;

uniform float Distortion;
uniform float Whiteout;

in vec2 texCoord;

out vec4 fragColor;

void main() {

    /*
     * UV：
     *
     * 0,0 ---- 1,0
     *  |        |
     *  |  0.5   |
     *  |        |
     * 0,1 ---- 1,1
     */

    vec2 centered =
        texCoord
        - vec2(0.5);

    /*
     * 修正寬螢幕比例，
     * 否則漩渦會被壓成橢圓。
     */
    float aspect =
        OutSize.x
        / OutSize.y;

    centered.x *= aspect;

    float radius =
        length(centered);

    float angle =
        atan(
            centered.y,
            centered.x
        );

    /*
     * 中央扭曲較強，
     * 邊緣較弱。
     */
    float centerStrength =
        1.0
        - smoothstep(
            0.0,
            0.85,
            radius
        );

    /*
     * Distortion:
     *
     * 0   = 正常
     * 0.5 = 明顯
     * 1   = 強烈
     * 1.5 = 驅逐階段
     */
    float twist =
        Distortion
        * centerStrength
        * 5.0;

    angle += twist;

    vec2 warped;

    warped.x =
        cos(angle)
        * radius;

    warped.y =
        sin(angle)
        * radius;

    warped.x /= aspect;

    warped += vec2(0.5);

    /*
     * 防止取樣超出畫面。
     */
    warped =
        clamp(
            warped,
            vec2(0.001),
            vec2(0.999)
        );

    vec4 color =
        texture(
            DiffuseSampler,
            warped
        );

    /*
     * 最終白光。
     */
    color.rgb =
        mix(
            color.rgb,
            vec3(1.0),
            Whiteout
        );

    fragColor =
        vec4(
            color.rgb,
            1.0
        );
}