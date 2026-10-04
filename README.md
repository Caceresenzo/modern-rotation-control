# Modern Rotation Control

An improved clone of [Crape Myrtle's Rotation Control](https://play.google.com/store/apps/details?id=org.crape.rotationcontrol&hl=en), rewritten from scratch but heavily inspired by the original, from design to features.

<table>
  <thead>
    <tr>
      <td>Settings</td>
      <td>Notification</td>
      <td>Smart Tile Icon</td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/464676ff-1056-4f76-a206-7e3beba11dee" width="300px" /></td>
      <td><img src="https://github.com/user-attachments/assets/ac7f491c-018a-45f1-9d0f-a2831fd0e0b5" width="300px" /></td>
      <td><img src="https://github.com/user-attachments/assets/ee38841a-35a5-49bc-988a-d2420f1a8de2" width="300px" /></td>
    </tr>
  </tbody>
</table>

## Motivation

I originally only wanted to redesign the notification, but the application has since grown many more features.
It started as a fun challenge, and I'm amazed that other people are using it!

## Features

### Rotation via Notification

The legacy approach, kept for feature parity with Crape Myrtle's version.

Why is it considered legacy?
- Since Android 12, custom notification layouts are heavily restricted.
- Since Android 14, ongoing notifications can be dismissed by the user.

Together, these changes make a notification a poor way to control rotation.

### Rotation via Quick Settings Tile

Since Android 7, apps can provide their own Quick Settings tiles, which makes it possible to replace the system tile with one that has extended behavior.

For the best experience, hide the system rotation tile and use the one provided by Modern Rotation Control instead.

### Rotation via Suggestions

Android 9 introduced a small button that lets you rotate the screen when automatic rotation is disabled.

Modern Rotation Control extends it with its configurable rotation modes, a left-handed mode, and a bigger button.

### Material 3

Modern Rotation Control follows Material 3 design guidelines and supports dynamic colors (Material You).
All menus, the notification, and the tile with its dialog adapt to your selected system colors.

### Automatic Lock

Automatically lock your device's rotation after a configurable delay.

### Per-Application Configuration

Assign a rotation mode to specific applications. It temporarily overrides the current mode while that application is in the foreground.

Some default applications, such as the launcher, are preconfigured to use portrait, but you can revert this.

This is especially useful for applications that only support a specific orientation.

> [!NOTE]
> This feature requires an Accessibility Service.

## Contributing

Feel free to [open an issue](https://github.com/Caceresenzo/modern-rotation-control/issues) if you'd like to request a feature.

I don't accept pull requests, especially AI-generated ones, as I want to keep developing this passion project entirely on my own.

New features can take a while to ship: I use each one myself for a few weeks first, fixing bugs and polishing it as much as possible before releasing it publicly.

## License

MIT
