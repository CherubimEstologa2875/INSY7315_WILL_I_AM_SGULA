# Build stage
# The .NET SDK image compiles the ASP.NET Core application
# (Microsoft, 2026).
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build

WORKDIR /src

# The build context is the INSY7315-WEBSITE directory.
# Therefore the solution/project paths are relative to that directory.
COPY ["INSY7315-WEBSITE/INSY7315-WEBSITE.csproj", "INSY7315-WEBSITE/"]

RUN dotnet restore "INSY7315-WEBSITE/INSY7315-WEBSITE.csproj"

COPY . .

WORKDIR "/src/INSY7315-WEBSITE"

RUN dotnet publish "INSY7315-WEBSITE.csproj" \
    -c Release \
    -o /app/publish \
    /p:UseAppHost=false


# Runtime stage
# The ASP.NET Core runtime image is smaller than the SDK image
# and is intended for running the published application
# (Microsoft, 2026).
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final

WORKDIR /app

COPY --from=build /app/publish .

# Cloud Run sends HTTP traffic to the configured container port.
ENV ASPNETCORE_URLS=http://0.0.0.0:8080

EXPOSE 8080

ENTRYPOINT ["dotnet", "INSY7315-WEBSITE.dll"]


# Reference List
# Microsoft, 2026. .NET container images. [online]
# Available at:
# https://learn.microsoft.com/en-us/dotnet/core/docker/container-images
# [Accessed 28 September 2026].
#
# Google Cloud, 2026. Build container images.
# [online] Available at:
# https://docs.cloud.google.com/build/docs/building/build-containers
# [Accessed 28 September 2026].