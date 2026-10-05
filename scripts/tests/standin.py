# A local stand-in for the sites the upload step sends to. It listens on localhost only, records
# every request it gets, and answers the way each site would. Modrinth's API is under /modrinth,
# CurseForge's upload API under /cf-upload and its website's file listing under /cf-site.
import json
import re
import threading
from email.parser import BytesParser
from email.policy import HTTP
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# What CurseForge's /api/game/version-types and /api/game/versions answer, trimmed to the entries
# that matter: 26.1.2 exists both as a Minecraft version and as a Bukkit one, and every file names
# its environments, Client and Server.
VERSION_TYPES = [{"id": 1, "name": "Minecraft 26.1", "slug": "minecraft-26-1"},
                 {"id": 2, "name": "Bukkit", "slug": "bukkit"},
                 {"id": 3, "name": "Modloader", "slug": "modloader"},
                 {"id": 4, "name": "Environment", "slug": "environment"}]
GAME_VERSIONS = [{"id": 101, "gameVersionTypeID": 1, "name": "26.1.2", "slug": "26-1-2"},
                 {"id": 102, "gameVersionTypeID": 1, "name": "26.1.1", "slug": "26-1-1"},
                 {"id": 201, "gameVersionTypeID": 2, "name": "26.1.2", "slug": "26-1-2"},
                 {"id": 301, "gameVersionTypeID": 3, "name": "NeoForge", "slug": "neoforge"},
                 {"id": 302, "gameVersionTypeID": 3, "name": "Forge", "slug": "forge"},
                 {"id": 401, "gameVersionTypeID": 4, "name": "Client", "slug": "client"},
                 {"id": 402, "gameVersionTypeID": 4, "name": "Server", "slug": "server"}]


class Request:
    def __init__(self, method, path, headers, body):
        self.method, self.path, self.headers, self.body = method, path, headers, body

    def parts(self):
        """The multipart form's parts, by name: (content bytes, filename)."""
        head = f"Content-Type: {self.headers['Content-Type']}\r\n\r\n".encode()
        message = BytesParser(policy=HTTP).parsebytes(head + self.body)
        return {p.get_param("name", header="content-disposition"): (p.get_payload(decode=True), p.get_filename())
                for p in message.iter_parts()}


class StandIn:
    """`modrinth[project]` and `curseforge[project]` hold the versions and file names each site has.
    A site whose name is in `broken` answers every request with a 500."""

    def __init__(self):
        self.requests = []
        self.modrinth = {}
        self.curseforge = {}
        self.broken = set()
        stand_in = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def _record(self):
                body = self.rfile.read(int(self.headers.get("Content-Length") or 0))
                request = Request(self.command, self.path, self.headers, body)
                stand_in.requests.append(request)
                return request

            def _answer(self, status, payload):
                data = json.dumps(payload).encode()
                self.send_response(status)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)

            def _route(self):
                request = self._record()
                site = request.path.split("/")[1]
                if site in stand_in.broken:
                    return self._answer(500, {"error": "stand-in broken"})
                path = request.path.split("?")[0]
                for pattern, handler in routes:
                    match = re.fullmatch(pattern, f"{request.method} {path}")
                    if match:
                        return self._answer(*handler(request, *match.groups()))
                self._answer(404, {"error": "not_found"})

            do_GET = do_POST = _route

        def modrinth_versions(request, project):
            return 200, [{"version_number": v} for v in stand_in.modrinth.get(project, [])]

        def modrinth_create(request):
            data = json.loads(request.parts()["data"][0])
            stand_in.modrinth.setdefault(data["project_id"], []).append(data["version_number"])
            return 200, {"id": "standin", "version_number": data["version_number"]}

        def cf_files(request, project):
            # A project with no files yet answers with an empty listing and no pagination.
            if not stand_in.curseforge.get(project):
                return 200, {"data": [], "pagination": {}}
            match = re.search(r"pageIndex=(\d+)", request.path)
            index = int(match.group(1)) if match else 0
            files = [{"fileName": f, "displayName": f} for f in stand_in.curseforge[project]]
            page = files[index * 2:index * 2 + 2]  # small pages, so the step must follow the pagination
            return 200, {"data": page, "pagination": {"index": index, "pageSize": 2, "totalCount": len(files)}}

        def cf_upload(request, project):
            stand_in.curseforge.setdefault(project, []).append(request.parts()["file"][1])
            return 200, {"id": 4242}

        routes = [
            (r"GET /modrinth/project/([^/]+)/version", modrinth_versions),
            (r"POST /modrinth/version", modrinth_create),
            (r"GET /cf-upload/api/game/version-types", lambda r: (200, VERSION_TYPES)),
            (r"GET /cf-upload/api/game/versions", lambda r: (200, GAME_VERSIONS)),
            (r"POST /cf-upload/api/projects/(\d+)/upload-file", cf_upload),
            (r"GET /cf-site/api/v1/mods/(\d+)/files", cf_files),
        ]

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.url = f"http://127.0.0.1:{self.server.server_address[1]}"
        threading.Thread(target=self.server.serve_forever, daemon=True).start()

    def sent(self, method, prefix):
        return [r for r in self.requests if r.method == method and r.path.startswith(prefix)]

    def close(self):
        self.server.shutdown()
        self.server.server_close()
