const BACKEND_ORIGIN = "__BACKEND_ORIGIN__";

const isBackendPath = (pathname) =>
  pathname === "/upload" ||
  pathname.startsWith("/api/") ||
  pathname.startsWith("/ws/");

const withBrowserSecurityHeaders = (response, noCache = false) => {
  const headers = new Headers(response.headers);
  headers.set("X-Content-Type-Options", "nosniff");
  headers.set("X-Frame-Options", "SAMEORIGIN");
  headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
  if (noCache) {
    headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
  }
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers,
  });
};

const proxyToBackend = async (request) => {
  const publicUrl = new URL(request.url);
  const backendUrl = new URL(
    `${publicUrl.pathname}${publicUrl.search}`,
    `${BACKEND_ORIGIN}/`,
  );
  const headers = new Headers(request.headers);

  // The Worker is the same-origin reverse proxy. Do not forward browser CORS
  // metadata or the Pages host as if this were a direct cross-origin request.
  headers.delete("host");
  headers.delete("origin");
  headers.delete("referer");
  headers.set("X-Forwarded-Host", publicUrl.host);
  headers.set("X-Forwarded-Proto", publicUrl.protocol.slice(0, -1));

  const init = {
    method: request.method,
    headers,
    redirect: "manual",
  };
  if (request.method !== "GET" && request.method !== "HEAD") {
    init.body = request.body;
  }

  try {
    const response = await fetch(backendUrl, init);
    // A WebSocket upgrade response carries a runtime-only webSocket property;
    // return it untouched so the browser can establish the connection.
    if (response.status === 101) {
      return response;
    }

    const responseHeaders = new Headers(response.headers);
    const location = responseHeaders.get("Location");
    if (location && location.startsWith(BACKEND_ORIGIN)) {
      responseHeaders.set(
        "Location",
        `${publicUrl.origin}${location.slice(BACKEND_ORIGIN.length)}`,
      );
    }
    return new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: responseHeaders,
    });
  } catch (error) {
    return Response.json(
      {
        code: 503,
        message: "演示服务暂时离线，请确认演示电脑与 Docker 已启动。",
      },
      { status: 503 },
    );
  }
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (isBackendPath(url.pathname)) {
      return proxyToBackend(request);
    }

    const assetResponse = await env.ASSETS.fetch(request);
    const isDocument =
      request.method === "GET" &&
      (request.headers.get("Accept") || "").includes("text/html");
    const contentType = assetResponse.headers.get("Content-Type") || "";

    // Cloudflare Pages has a built-in SPA fallback when no 404.html exists.
    // Turn that fallback back into a real 404 for missing scripts/images/API-
    // unrelated resources, otherwise an <img> request receives index.html.
    if (
      !isDocument &&
      assetResponse.status === 200 &&
      contentType.includes("text/html") &&
      url.pathname !== "/" &&
      url.pathname !== "/index.html"
    ) {
      return withBrowserSecurityHeaders(
        new Response("Not Found", {
          status: 404,
          headers: { "Content-Type": "text/plain; charset=utf-8" },
        }),
      );
    }

    if (assetResponse.status !== 404 || !isDocument) {
      return withBrowserSecurityHeaders(
        assetResponse,
        url.pathname === "/" || url.pathname === "/index.html",
      );
    }

    // Vue Router uses history mode. Unknown document routes must fall back to
    // index.html, while missing images/scripts keep their real 404 response.
    const indexUrl = new URL("/index.html", request.url);
    const indexRequest = new Request(indexUrl, request);
    const indexResponse = await env.ASSETS.fetch(indexRequest);
    return withBrowserSecurityHeaders(indexResponse, true);
  },
};
